package com.github.davidmarquis.redisscheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PollingThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);

    private TaskRunner runner;
    private int maxRetriesOnConnectionFailure;
    private int pollingDelayMillis;

    private boolean stopRequested = false;
    private int numRetriesAttempted = 0;

    PollingThread(TaskRunner runner, int maxRetriesOnConnectionFailure, int pollingDelayMillis) {
        this.runner = runner;
        this.maxRetriesOnConnectionFailure = maxRetriesOnConnectionFailure;
        this.pollingDelayMillis = pollingDelayMillis;
    }

    void requestStop() {
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
                            "No additional scheduled task will be triggered until the application is restarted.", getName()), e);
        }

        if (isMaxRetriesAttemptsReached()) {
            log.error(String.format("[%s] Maximum number of retries (%s) after Redis connection failure has been reached. " +
                                            "No additional scheduled task will be triggered until the application is restarted.", getName(), maxRetriesOnConnectionFailure));
        } else {
            log.info(String.format("[%s] Redis Scheduler stopped", getName()));
        }
    }

    private void attemptTriggerNextTask() throws InterruptedException {
        try {
            boolean taskTriggered = runner.triggerNextTaskIfFound();

            // if a task was triggered, we'll try again immediately. This will help to speed up the execution
            // process if a few tasks were due for execution.
            if (!taskTriggered) {
                sleep(pollingDelayMillis);
            }

            resetRetriesAttemptsCount();
        } catch (RedisConnectException e) {
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
