package com.github.davidmarquis.redisscheduler.impl;

import com.github.davidmarquis.redisscheduler.RedisTaskScheduler;
import com.github.davidmarquis.redisscheduler.TaskTriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Set;

public class RedisTaskSchedulerImpl implements RedisTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(RedisTaskSchedulerImpl.class);

    private static final String SCHEDULE_KEY = "redis-scheduler.%s";
    private static final String DEFAULT_SCHEDULER_NAME = "scheduler";

    private Clock clock = Clock.systemDefaultZone();
    private RedisTemplate redisTemplate;
    private TaskTriggerListener taskTriggerListener;

    /**
     * Delay between each polling of the scheduled tasks. The lower the value, the best precision in triggering tasks.
     * However, the lower the value, the higher the load on Redis.
     */
    private int pollingDelayMillis = 10000;

    /**
     * If you need multiple schedulers for the same application, customize their names to differentiate in logs.
     */
    private String schedulerName = DEFAULT_SCHEDULER_NAME;

    private PollingThread pollingThread;
    private int maxRetriesOnConnectionFailure = 1;

    @SuppressWarnings("unchecked")
    public void runNow(String taskId) {
        scheduleAt(taskId, clock.instant());
    }

    @SuppressWarnings("unchecked")
    public void scheduleAt(String taskId, Instant triggerTime) {
        if (triggerTime == null) {
            throw new IllegalArgumentException("A trigger time must be provided.");
        }

        redisTemplate.opsForZSet().add(keyForScheduler(), taskId, triggerTime.toEpochMilli());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unschedule(String taskId) {

        redisTemplate.opsForZSet().remove(keyForScheduler(), taskId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unscheduleAllTasks() {
        redisTemplate.delete(keyForScheduler());
    }

    @PostConstruct
    public void initialize() {
        pollingThread = new PollingThread();
        pollingThread.setName(schedulerName + "-polling");

        pollingThread.start();

        log.info(String.format("[%s] Started Redis Scheduler (polling freq: [%sms])", schedulerName, pollingDelayMillis));
    }

    @PreDestroy
    public void destroy() {
        if (pollingThread != null) {
            pollingThread.requestStop();
        }
    }

    @Override
    public void setTaskTriggerListener(TaskTriggerListener taskTriggerListener) {
        this.taskTriggerListener = taskTriggerListener;
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void setSchedulerName(String schedulerName) {
        this.schedulerName = schedulerName;
    }

    public void setPollingDelayMillis(int pollingDelayMillis) {
        this.pollingDelayMillis = pollingDelayMillis;
    }

    public void setMaxRetriesOnConnectionFailure(int maxRetriesOnConnectionFailure) {
        this.maxRetriesOnConnectionFailure = maxRetriesOnConnectionFailure;
    }

    private String keyForScheduler() {
        return String.format(SCHEDULE_KEY, schedulerName);
    }

    @SuppressWarnings("unchecked")
    private boolean triggerNextTaskIfFound() {

        return (Boolean) redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                boolean taskWasTriggered = false;
                final String key = keyForScheduler();

                redisOperations.watch(key);

                String task = findFirstTaskDueForExecution(redisOperations);

                if (task == null) {
                    redisOperations.unwatch();
                } else {
                    redisOperations.multi();
                    redisOperations.opsForZSet().remove(key, task);
                    boolean executionSuccess = (redisOperations.exec() != null);

                    if (executionSuccess) {
                        log.debug(String.format("[%s] Triggering execution of task [%s]", schedulerName, task));
                        tryTaskExecution(task);
                        taskWasTriggered = true;
                    } else {
                        log.warn(String.format("[%s] Race condition detected for triggering of task [%s]. " +
                                "The task has probably been triggered by another instance of this application.", schedulerName, task));
                    }
                }

                return taskWasTriggered;
            }
        });
    }

    private void tryTaskExecution(String task) {
        try {
            taskTriggerListener.taskTriggered(task);
        } catch (Exception e) {
            log.error(String.format("[%s] Error during execution of task [%s]", schedulerName, task), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String findFirstTaskDueForExecution(RedisOperations ops) {
        final long minScore = 0;
        final long maxScore = clock.millis();

        // we unfortunately need to go wild here, the default API does not allow us to limit the number
        // of items returned by the ZRANGEBYSCORE operation.
        Set<byte[]> found = (Set<byte[]>) ops.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                String key = keyForScheduler();
                return redisConnection.zRangeByScore(key.getBytes(), minScore, maxScore, 0, 1);
            }
        });

        String foundTask = null;
        if (found != null && !found.isEmpty()) {
            byte[] valueRaw = found.iterator().next();
            Object valueObj = ops.getValueSerializer().deserialize(valueRaw);
            foundTask = (valueObj != null) ? valueObj.toString() : null;
        }
        return foundTask;
    }

    private class PollingThread extends Thread {
        private boolean stopRequested = false;
        private int numRetriesAttempted = 0;

        public void requestStop() {
            stopRequested = true;
        }

        @Override
        public void run() {
            try {
                while (!stopRequested && !isMaxRetriesAttemptsReached()) {

                    try {
                        attemptTriggerNextTask();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.error(String.format(
                        "[%s] Error while polling scheduled tasks. " +
                        "No additional scheduled task will be triggered until the application is restarted.", schedulerName), e);
            }

            if (isMaxRetriesAttemptsReached()) {
                log.error(String.format("[%s] Maximum number of retries (%s) after Redis connection failure has been reached. " +
                        "No additional scheduled task will be triggered until the application is restarted.", schedulerName, maxRetriesOnConnectionFailure));
            } else {
                log.info("[%s] Redis Scheduler stopped");
            }
        }

        private void attemptTriggerNextTask() throws InterruptedException {
            try {
                boolean taskTriggered = triggerNextTaskIfFound();

                // if a task was triggered, we'll try again immediately. This will help to speed up the execution
                // process if a few tasks were due for execution.
                if (!taskTriggered) {
                    sleep(pollingDelayMillis);
                }

                resetRetriesAttemptsCount();
            } catch (RedisConnectionFailureException e) {
                incrementRetriesAttemptsCount();
                log.warn(String.format("Connection failure during scheduler polling (attempt %s/%s)", numRetriesAttempted, maxRetriesOnConnectionFailure));
            }
        }

        private boolean isMaxRetriesAttemptsReached() {
            return numRetriesAttempted >= maxRetriesOnConnectionFailure;
        }

        private void resetRetriesAttemptsCount() {
            numRetriesAttempted = 0;
        }

        private void incrementRetriesAttemptsCount() {
            numRetriesAttempted++;
        }
    }
}
