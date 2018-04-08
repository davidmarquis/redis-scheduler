package com.github.davidmarquis.redisscheduler;

import com.github.davidmarquis.redisscheduler.drivers.jedis.JedisDriver;
import org.junit.After;
import org.junit.AfterClass;
import redis.clients.jedis.JedisPool;

public class JedisIntegrationTest extends AcceptanceTestSuite {

    private static final JedisPool pool = new JedisPool("localhost", 6379);

    @Override
    protected void provideActors() {
        scheduler = new RedisTaskScheduler(new JedisDriver(pool), taskTriggerListener);
        scheduler.setSchedulerName("jedis-scheduler");
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
        pool.close();
    }
}
