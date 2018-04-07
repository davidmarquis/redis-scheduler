package com.github.davidmarquis.redisscheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public class RedisTaskScheduler implements TaskScheduler, TaskRunner {

    private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);

    private static final String DEFAULT_SCHEDULER_NAME = "scheduler";

    private Clock clock = Clock.systemDefaultZone();
    private RedisDriver driver;
    private TaskTriggerListener listener;

    /**
     * If you need multiple schedulers for the same application, customize their names to differentiate in logs.
     */
    private SchedulerIdentity identity = SchedulerIdentity.of(DEFAULT_SCHEDULER_NAME);

    private PollingThread pollingThread;
    /**
     * Delay between each polling of the scheduled tasks. The lower the value, the best precision in triggering tasks.
     * However, the lower the value, the higher the load on Redis.
     */
    private int pollingDelayMillis = 10000;
    private int maxRetriesOnConnectionFailure = 1;

    public RedisTaskScheduler(RedisDriver driver, TaskTriggerListener listener) {
        this.driver = driver;
        this.listener = listener;
    }

    @SuppressWarnings("unchecked")
    public void runNow(String taskId) {
        scheduleAt(taskId, clock.instant());
    }

    @SuppressWarnings("unchecked")
    public void scheduleAt(String taskId, Instant triggerTime) {
        if (triggerTime == null) {
            throw new IllegalArgumentException("A trigger time must be provided.");
        }

        driver.execute(commands -> commands.addToSetWithScore(identity.key(), taskId, triggerTime.toEpochMilli()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unschedule(String taskId) {
        driver.execute(commands -> commands.removeFromSet(identity.key(), taskId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unscheduleAllTasks() {
        driver.execute(commands -> commands.remove(identity.key()));
    }

    public void stop() {
        close();
    }

    @Override
    @PreDestroy
    public void close() {
        if (pollingThread != null) {
            pollingThread.requestStop();
        }
    }

    public void start() {
        initialize();
    }

    @PostConstruct
    public void initialize() {
        pollingThread = new PollingThread(this, maxRetriesOnConnectionFailure, pollingDelayMillis);
        pollingThread.setName(identity.name() + "-polling");

        pollingThread.start();

        log.info(String.format("[%s] Started Redis Scheduler (polling freq: [%sms])", identity.name(), pollingDelayMillis));
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void setSchedulerName(String schedulerName) {
        this.identity = SchedulerIdentity.of(schedulerName);
    }

    public void setPollingDelayMillis(int pollingDelayMillis) {
        this.pollingDelayMillis = pollingDelayMillis;
    }

    public void setMaxRetriesOnConnectionFailure(int maxRetriesOnConnectionFailure) {
        this.maxRetriesOnConnectionFailure = maxRetriesOnConnectionFailure;
    }

    @SuppressWarnings("unchecked")
    public boolean triggerNextTaskIfFound() {
        return driver.fetch(commands -> {
            boolean taskWasTriggered = false;

            commands.watch(identity.key());

            Optional<String> nextTask = commands.firstByScore(identity.key(), 0, clock.millis());

            if (nextTask.isPresent()) {
                String nextTaskId = nextTask.get();

                commands.multi();
                commands.removeFromSet(identity.key(), nextTaskId);
                boolean executionSuccess = commands.exec();

                if (executionSuccess) {
                    log.debug(String.format("[%s] Triggering execution of task [%s]", identity.name(), nextTaskId));

                    tryTaskExecution(nextTaskId);
                    taskWasTriggered = true;
                } else {
                    log.warn(String.format("[%s] Race condition detected for triggering of task [%s]. " +
                                                   "The task has probably been triggered by another instance of this application.", identity.name(), nextTaskId));
                }
            } else {
                commands.unwatch();
            }

            return taskWasTriggered;
        });
    }

    private void tryTaskExecution(String task) {
        try {
            listener.taskTriggered(task);
        } catch (Exception e) {
            log.error(String.format("[%s] Error during execution of task [%s]", identity.name(), task), e);
        }
    }
}
