package com.github.davidmarquis.redisscheduler;

/**
 * Callback interface that will get executed when a previously-scheduled task is due for execution.
 */
public interface TaskTriggerListener {

    /**
     * Called by the scheduler once a task is due for execution.
     * @param taskId the task ID that was originally submitted to the RedisTaskScheduler.
     */
    void taskTriggered(String taskId);
}
