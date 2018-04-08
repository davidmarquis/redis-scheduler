package com.github.davidmarquis.redisscheduler;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface RedisDriver {

    <T> T fetch(Function<Commands, T> block);

    default void execute(Consumer<Commands> block) {
        fetch((Function<Commands, Void>) commands -> {
            block.accept(commands);
            return null;
        });
    }

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
