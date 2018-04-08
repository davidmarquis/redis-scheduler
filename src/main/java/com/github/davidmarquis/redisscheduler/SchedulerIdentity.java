package com.github.davidmarquis.redisscheduler;

class SchedulerIdentity {
    private static final String REDIS_KEY_FORMAT = "redis-scheduler.%s";

    private String name;

    private SchedulerIdentity(String name) {
        this.name = name;
    }

    String key() {
        return String.format(REDIS_KEY_FORMAT, name);
    }

    String name() {
        return name;
    }

    static SchedulerIdentity of(String name) {
        return new SchedulerIdentity(name);
    }
}
