package com.github.redisscheduler.impl;

import com.github.redisscheduler.TaskTriggerListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Special trigger listener that allows tests to wait until a certain number of tasks have been triggered
 * by the scheduler. This is necessary due to the asynchronous nature of the scheduler.
 */
public class LatchedTriggerListener implements TaskTriggerListener {

    private CountDownLatch latch;

    private List<String> triggeredTasks = new ArrayList<String>();

    @Override
    public void taskTriggered(String taskId) {
        triggeredTasks.add(taskId);

        if (latch != null) {
            latch.countDown();
        }
    }

    public List<String> getTriggeredTasks() {
        return triggeredTasks;
    }

    public void waitUntilTriggered(int nTimes, int timeoutMillis) throws InterruptedException {
        int remainingCount = nTimes - triggeredTasks.size();
        if (remainingCount < 1) {
            return;
        }

        latch = new CountDownLatch(remainingCount);
        latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public void reset() {
        triggeredTasks.clear();
        latch = null;
    }
}
