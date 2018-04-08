package com.github.davidmarquis.redisscheduler.lib;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import redis.embedded.RedisServer;

public class StartRedis implements TestRule {
    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                RedisServer server = new RedisServer();
                server.start();

                try {
                    base.evaluate();
                } finally {
                    server.stop();
                }
            }
        };
    }
}
