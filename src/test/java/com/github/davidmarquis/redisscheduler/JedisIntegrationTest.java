package com.github.davidmarquis.redisscheduler;

import com.github.davidmarquis.redisscheduler.AcceptanceTestSuite;
import com.github.davidmarquis.redisscheduler.RedisTaskScheduler;
import com.github.davidmarquis.redisscheduler.drivers.jedis.JedisDriver;
import org.junit.After;
import org.junit.AfterClass;
import redis.clients.jedis.JedisPool;

import java.io.IOException;

public class JedisIntegrationTest extends AcceptanceTestSuite {

    private static final JedisPool pool = new JedisPool("localhost", 6379);

    @Override
    protected void provideActors() {
        RedisTaskScheduler scheduler = new RedisTaskScheduler(new JedisDriver(pool), taskTriggerListener);
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
        pool.close();
    }
}
