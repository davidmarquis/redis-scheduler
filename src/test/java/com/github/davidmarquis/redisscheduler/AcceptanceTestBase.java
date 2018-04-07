package com.github.davidmarquis.redisscheduler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AcceptanceTestBase {

    protected TaskScheduler scheduler;
    protected StubbedClock clock = new StubbedClock();
    protected LatchedTriggerListener taskTriggerListener = new LatchedTriggerListener();

    protected abstract void provideDependencies();

    @Before
    public void setup() {
        provideDependencies();

        scheduler.unscheduleAllTasks();
        taskTriggerListener.reset();

        clock.is("20180405 10:00");
    }

    @After
    public void tearDown() {
        scheduler.unscheduleAllTasks();
        taskTriggerListener.reset();
    }

    @Test
    public void canTriggerTask() throws InterruptedException {
        scheduler.scheduleAt("mytask", clock.in(2, HOURS));
        clock.fastForward(2, HOURS);

        checkExactTasksTriggered("mytask");
    }

    @Test
    public void canTriggerMultipleTasks() throws InterruptedException {
        scheduler.scheduleAt("first", clock.in(1, HOURS));
        scheduler.scheduleAt("second", clock.in(2, HOURS));
        clock.fastForward(2, HOURS);

        checkExactTasksTriggered("first", "second");
    }

    @Test
    public void canTriggerPastTasks() throws InterruptedException {
        scheduler.scheduleAt("mytask1", clock.in(1, HOURS));
        scheduler.scheduleAt("mytask2", clock.in(2, HOURS));
        scheduler.scheduleAt("mytask3", clock.in(5, HOURS));
        clock.fastForward(3, HOURS);

        checkOnlyTasksTriggered("mytask1", "mytask2");
    }

    @Test
    public void cannotTriggerFutureTasks() throws InterruptedException {
        scheduler.scheduleAt("mytask", clock.in(1, HOURS));

        assertNoTaskTriggered();
    }

    @Test
    public void canScheduleInThePast() throws InterruptedException {
        scheduler.scheduleAt("mytask", clock.in(-1, HOURS));

        checkExactTasksTriggered("mytask");
    }

    @Test(expected = IllegalArgumentException.class)
    public void schedulingAtNullTimeRaisesException() {
        scheduler.scheduleAt("mytask", null);
    }

    @Test
    public void runNowImmediatelyTriggers() throws InterruptedException {
        scheduler.runNow("immediate");

        checkExactTasksTriggered("immediate");
    }

    @Test
    public void canRescheduleTask() throws InterruptedException {
        scheduler.scheduleAt("mytask", clock.in(5, HOURS));
        scheduler.scheduleAt("mytask", clock.in(1, HOURS));
        clock.fastForward(2, HOURS);

        checkExactTasksTriggered("mytask");
    }

    @Test
    public void canUnscheduleTask() throws InterruptedException {
        scheduler.scheduleAt("mytask1", clock.in(1, HOURS));
        scheduler.scheduleAt("mytask2", clock.in(1, HOURS));
        scheduler.unschedule("mytask2");
        clock.fastForward(2, HOURS);

        checkOnlyTasksTriggered("mytask1");
    }

    private void checkExactTasksTriggered(String... tasks) throws InterruptedException {
        taskTriggerListener.waitUntilTriggeredCount(tasks.length, 1000);

        assertTasksTriggered(tasks);
    }

    private void checkOnlyTasksTriggered(String... tasks) throws InterruptedException {
        // if only a subset of the scheduled tasks are expected to be triggered, then we need to wait for a while.
        Thread.sleep(1000);

        assertTasksTriggered(tasks);
    }

    private void assertTasksTriggered(String... tasks) {
        List<String> triggeredTasks = taskTriggerListener.getTriggeredTasks();
        assertThat("Triggered tasks count", triggeredTasks.size(), is(tasks.length));
        assertThat("Triggered tasks", triggeredTasks, is(asList(tasks)));
    }

    private void assertNoTaskTriggered() throws InterruptedException {
        Thread.sleep(1000);

        assertThat("No tasks should triggered", taskTriggerListener.getTriggeredTasks().size(), is(0));
    }
}
