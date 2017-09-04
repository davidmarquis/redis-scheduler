package com.github.davidmarquis.redisscheduler;


import com.github.davidmarquis.redisscheduler.impl.LatchedTriggerListener;
import com.github.davidmarquis.redisscheduler.impl.StubbedClock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.text.ParseException;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/application-context-test.xml")
public class RedisTaskSchedulerIntegrationTest {

    @Autowired
    private RedisTaskScheduler scheduler;

    @Autowired
    private StubbedClock clock;

    @Autowired
    private LatchedTriggerListener taskTriggerListener;

    @Before
    public void setup() {
        scheduler.unscheduleAllTasks();
        taskTriggerListener.reset();
    }

    @Test
    public void canTriggerTask() throws ParseException, InterruptedException {

        // given
        clock.is("20131101 10:00");

        // when
        scheduler.scheduleAt("mytask", clock.in(2, HOURS));
        clock.fastForward(2, HOURS);

        // then
        assertTasksTriggered("mytask");
    }

    @Test
    public void canTriggerMultipleTasks() throws ParseException, InterruptedException {

        // given
        clock.is("20131101 10:00");

        // when
        scheduler.scheduleAt("mytask1", clock.in(1, HOURS));
        scheduler.scheduleAt("mytask2", clock.in(2, HOURS));
        clock.fastForward(2, HOURS);

        // then
        assertTasksTriggered("mytask1", "mytask2");
    }

    @Test
    public void canTriggerPastTasks() throws ParseException, InterruptedException {

        // given
        clock.is("20131101 10:00");

        // when
        scheduler.scheduleAt("mytask1", clock.in(1, HOURS));
        scheduler.scheduleAt("mytask2", clock.in(2, HOURS));
        scheduler.scheduleAt("mytask3", clock.in(5, HOURS));
        clock.fastForward(3, HOURS);

        // then
        assertOnlyTasksTriggered("mytask1", "mytask2");
    }

    @Test
    public void cannotTriggerFutureTasks() throws ParseException, InterruptedException {

        // given
        clock.is("20131101 10:00");

        // when
        scheduler.scheduleAt("mytask", clock.in(1, HOURS));

        // then
        assertNoTaskTriggered();
    }

    @Test
    public void canScheduleInThePast() throws ParseException, InterruptedException {
        // given
        clock.is("20131101 10:00");

        // when
        scheduler.scheduleAt("mytask", clock.in(-1, HOURS));

        // then
        assertTasksTriggered("mytask");
    }

    @Test(expected = IllegalArgumentException.class)
    public void schedulingAtNullTimeRaisesException() throws ParseException, InterruptedException {
        // when
        scheduler.scheduleAt("mytask", null);
    }

    @Test
    public void runNowImmediatelyTriggers() throws ParseException, InterruptedException {

        // given
        clock.is("20131101 10:00");

        // when
        scheduler.runNow("mytask");

        // then
        assertTasksTriggered("mytask");
    }

    @Test
    public void canRescheduleTask() throws ParseException, InterruptedException {

        // given
        clock.is("20131101 10:00");

        // when
        scheduler.scheduleAt("mytask", clock.in(5, HOURS));
        scheduler.scheduleAt("mytask", clock.in(1, HOURS));
        clock.fastForward(2, HOURS);

        // then
        assertTasksTriggered("mytask");
    }

    @Test
    public void canUnscheduleTask() throws ParseException, InterruptedException {

        // given
        clock.is("20131101 10:00");

        // when
        scheduler.scheduleAt("mytask1", clock.in(1, HOURS));
        scheduler.scheduleAt("mytask2", clock.in(1, HOURS));
        scheduler.unschedule("mytask2");
        clock.fastForward(2, HOURS);

        // then
        assertOnlyTasksTriggered("mytask1");
    }

    private void assertTasksTriggered(String... tasks) throws InterruptedException {
        // saves a little bit of time for tests execution when we know all tasks scheduled should be triggered.
        taskTriggerListener.waitUntilTriggered(tasks.length, 1000);

        assertThat("Triggered tasks count", taskTriggerListener.getTriggeredTasks().size(), is(tasks.length));
        assertThat("Triggered tasks", taskTriggerListener.getTriggeredTasks(), is(asList(tasks)));
    }

    private void assertOnlyTasksTriggered(String... tasks) throws InterruptedException {
        // if only a subset of the scheduled tasks are expected to be triggered, then we need to wait for a while.
        Thread.sleep(1000);

        assertThat("Triggered tasks count", taskTriggerListener.getTriggeredTasks().size(), is(tasks.length));
        assertThat("Triggered tasks", taskTriggerListener.getTriggeredTasks(), is(asList(tasks)));
    }

    private void assertNoTaskTriggered() throws InterruptedException {
        Thread.sleep(1000);

        assertThat("No tasks should triggered", taskTriggerListener.getTriggeredTasks().size(), is(0));
    }
}
