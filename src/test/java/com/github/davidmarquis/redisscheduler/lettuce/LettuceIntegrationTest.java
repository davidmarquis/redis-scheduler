package com.github.davidmarquis.redisscheduler.lettuce;

import com.github.davidmarquis.redisscheduler.AcceptanceTestBase;
import com.github.davidmarquis.redisscheduler.RedisTaskScheduler;
import com.github.davidmarquis.redisscheduler.drivers.lettuce.LettuceDriver;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import redis.embedded.RedisServer;

import java.io.IOException;

public class LettuceIntegrationTest extends AcceptanceTestBase {

    private static final RedisClient client = RedisClient.create(RedisURI.create("localhost", 6379));

    private static RedisServer server;

    @BeforeClass
    public static void startRedis() throws IOException {
        server = new RedisServer();
        server.start();
    }

    @Override
    protected void provideDependencies() {
        RedisTaskScheduler redisScheduler = new RedisTaskScheduler(new LettuceDriver(client), taskTriggerListener);
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
        client.shutdown();
        server.stop();
    }
}
