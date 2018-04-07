package com.github.davidmarquis.redisscheduler;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface RedisDriver {

    void execute(Consumer<Commands> block);
    <T> T fetch(Function<Commands, T> block);

    interface Commands {
        void addToSetWithScore(String key, String taskId, long score);
        void removeFromSet(String key, String taskId);
        void remove(String key);
        void watch(String key);
        void unwatch();
        void multi();
        boolean exec();
        Optional<String> firstByScore(String key, long minScore, long maxScore);
    }
}
