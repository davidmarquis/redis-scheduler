package com.github.davidmarquis.redisscheduler;

import java.io.Closeable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Calendar;

/**
 * Schedules arbitrary tasks in the future.
 */
public interface TaskScheduler extends Closeable {
    /**
     * Runs a task immediately.
     * @param taskId an arbitrary task identifier. That same identifier will be used in TaskTriggerListener callback
     *               once the task is due for execution.
     */
    void runNow(String taskId);

    /**
     * Schedules a task for future execution.
     * @param taskId an arbitrary task identifier. That same identifier will be used in TaskTriggerListener callback
     *               once the task is due for execution.
     * @param trigger the time at which we want the task to be executed. If this value is <code>null</code> or in the past,
     *                then the task will be immediately scheduled for execution.
     */
    void scheduleAt(String taskId, Instant trigger);

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
