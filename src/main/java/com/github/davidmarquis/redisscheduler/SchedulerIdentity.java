package com.github.davidmarquis.redisscheduler;

import org.slf4j.Logger;

public class SchedulerIdentity {
    private static final String REDIS_KEY_FORMAT = "redis-scheduler.%s";

    private String name;

    public SchedulerIdentity(String name) {
        this.name = name;
    }

    public String key() {
        return String.format(REDIS_KEY_FORMAT, name);
    }

    public String name() {
        return name;
    }

    public static SchedulerIdentity of(String name) {
        return new SchedulerIdentity(name);
    }
}
