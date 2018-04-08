package com.github.davidmarquis.redisscheduler;


import com.github.davidmarquis.redisscheduler.lib.LatchedTriggerListener;
import com.github.davidmarquis.redisscheduler.lib.StubbedClock;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/application-context-test.xml")
public class SpringIntegrationTest extends AcceptanceTestSuite {

    @Autowired
    private ApplicationContext ctx;

    @Override
    protected void provideActors() {
        scheduler = ctx.getBean(RedisTaskScheduler.class);
        clock = ctx.getBean(StubbedClock.class);
        taskTriggerListener = ctx.getBean(LatchedTriggerListener.class);
    }
}
