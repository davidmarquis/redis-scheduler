package com.github.davidmarquis.redisscheduler.lib;

import org.junit.rules.ExternalResource;
import redis.embedded.RedisServer;

public class StartRedis extends ExternalResource {

    private RedisServer server;

    @Override
    protected void before() throws Throwable {
        server = new RedisServer();
        server.start();
    }

    @Override
    protected void after() {
        server.stop();
    }
}
