package com.github.davidmarquis.redisscheduler;

import com.github.davidmarquis.redisscheduler.AcceptanceTestSuite;
import com.github.davidmarquis.redisscheduler.RedisTaskScheduler;
import com.github.davidmarquis.redisscheduler.drivers.lettuce.LettuceDriver;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.junit.After;
import org.junit.AfterClass;

import java.io.IOException;

public class LettuceIntegrationTest extends AcceptanceTestSuite {

    private static final RedisClient client = RedisClient.create(RedisURI.create("localhost", 6379));

    @Override
    protected void provideActors() {
        RedisTaskScheduler scheduler = new RedisTaskScheduler(new LettuceDriver(client), taskTriggerListener);
        scheduler.setClock(clock);
        scheduler.setPollingDelayMillis(50);
        scheduler.initialize();

        this.scheduler = scheduler;
    }

    @After
    public void closeScheduler() throws IOException {
        scheduler.close();
    }

    @AfterClass
    public static void shutdown() {
        client.shutdown();
    }
}
