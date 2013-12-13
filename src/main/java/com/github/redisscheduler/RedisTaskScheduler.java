package com.github.redisscheduler;

import java.util.Calendar;

/**
 * Schedules arbitrary tasks in the future.
 */
public interface RedisTaskScheduler {

    /**
     * @param taskTriggerListener the global listener that will be called when a task is due for execution.
     */
    void setTaskTriggerListener(TaskTriggerListener taskTriggerListener);

    /**
     * Schedules a task for future execution.
     * @param taskId an arbitrary task identifier. That same identifier will be used in the TaskTriggerListener callback
     *               once the task is due for execution.
     * @param trigger the time at which we want the task to be executed. If this value is <code>null</code> or in the past,
     *                then the task will be immediately scheduled for execution.
     */
    void schedule(String taskId, Calendar trigger);

    /**
     * Removes all currently scheduled tasks from the scheduler.
     */
    void unscheduleAllTasks();

    /**
     * Removes a specific task from the scheduler. If the task was not previously scheduled, then calling this method
     * has no particular effect.
     * @param taskId The task ID to remove.
     */
    void unschedule(String taskId);
}
