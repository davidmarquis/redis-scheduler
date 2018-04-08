package com.github.davidmarquis.redisscheduler;

import com.github.davidmarquis.redisscheduler.drivers.lettuce.LettuceDriver;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.junit.After;
import org.junit.AfterClass;

public class LettuceIntegrationTest extends AcceptanceTestSuite {

    private static final RedisClient client = RedisClient.create(RedisURI.create("localhost", 6379));

    @Override
    protected void provideActors() {
        scheduler = new RedisTaskScheduler(new LettuceDriver(client), taskTriggerListener);
        scheduler.setSchedulerName("lettuce-scheduler");
        scheduler.setClock(clock);
        scheduler.setPollingDelayMillis(50);
        scheduler.start();
    }

    @After
    public void stopScheduler() {
        scheduler.stop();
    }

    @AfterClass
    public static void shutdown() {
        client.shutdown();
    }
}
