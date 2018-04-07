package com.github.davidmarquis.redisscheduler.jedis;

import com.github.davidmarquis.redisscheduler.AcceptanceTestSuite;
import com.github.davidmarquis.redisscheduler.RedisTaskScheduler;
import com.github.davidmarquis.redisscheduler.drivers.jedis.JedisDriver;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import redis.clients.jedis.JedisPool;
import redis.embedded.RedisServer;

import java.io.IOException;

public class JedisIntegrationTest extends AcceptanceTestSuite {

    private static final JedisPool pool = new JedisPool("localhost", 6379);

    private static RedisServer server;

    @BeforeClass
    public static void startRedis() throws IOException {
        server = new RedisServer();
        server.start();
    }

    @Override
    protected void provideDependencies() {
        RedisTaskScheduler redisScheduler = new RedisTaskScheduler(new JedisDriver(pool), taskTriggerListener);
        redisScheduler.setClock(clock);
        redisScheduler.setPollingDelayMillis(50);
        redisScheduler.initialize();

        scheduler = redisScheduler;
    }

    @After
    public void closeScheduler() throws IOException {
        scheduler.close();
    }

    @AfterClass
    public static void shutdown() {
        pool.close();
        server.stop();
    }
}
