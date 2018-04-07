package com.github.davidmarquis.redisscheduler;

public interface TaskRunner {
    boolean triggerNextTaskIfFound();
}
