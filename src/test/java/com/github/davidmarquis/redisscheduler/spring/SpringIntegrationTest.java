package com.github.davidmarquis.redisscheduler.spring;


import com.github.davidmarquis.redisscheduler.AcceptanceTestBase;
import com.github.davidmarquis.redisscheduler.TaskScheduler;
import com.github.davidmarquis.redisscheduler.LatchedTriggerListener;
import com.github.davidmarquis.redisscheduler.StubbedClock;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/application-context-test.xml")
public class SpringIntegrationTest extends AcceptanceTestBase {

    @Autowired
    private ApplicationContext ctx;

    @Override
    protected void provideDependencies() {
        scheduler = ctx.getBean(TaskScheduler.class);
        clock = ctx.getBean(StubbedClock.class);
        taskTriggerListener = ctx.getBean(LatchedTriggerListener.class);
    }
}
