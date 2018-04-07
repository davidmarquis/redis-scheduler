package com.github.davidmarquis.redisscheduler.drivers.lettuce;

import com.github.davidmarquis.redisscheduler.RedisConnectException;
import com.github.davidmarquis.redisscheduler.RedisDriver;
import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.Optional;
import java.util.function.Function;

/**
 * Driver using Lettuce in synchronous mode (see lettuce.io)
 */
public class LettuceDriver implements RedisDriver {
    private final RedisClient client;

    public LettuceDriver(RedisClient client) {
        this.client = client;
    }

    @Override
    public <T> T fetch(Function<Commands, T> block) {
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            return block.apply(new LettuceCommands(commands));
        } catch (RedisConnectionException e) {
            throw new RedisConnectException(e);
        }
    }

    private static class LettuceCommands implements Commands {
        private final RedisCommands<String, String> commands;

        private LettuceCommands(RedisCommands<String, String> commands) {
            this.commands = commands;
        }

        @Override
        public void addToSetWithScore(String key, String taskId, long score) {
            commands.zadd(key, score, taskId);
        }

        @Override
        public void removeFromSet(String key, String taskId) {
            commands.zrem(key, taskId);
        }

        @Override
        public void remove(String key) {
            commands.del(key);
        }

        @Override
        public void watch(String key) {
            commands.watch(key);
        }

        @Override
        public void unwatch() {
            commands.unwatch();
        }

        @Override
        public void multi() {
            commands.multi();
        }

        @Override
        public boolean exec() {
            return !commands.exec().isEmpty();
        }

        @Override
        public Optional<String> firstByScore(String key, long minScore, long maxScore) {
            return commands.zrangebyscore(key, Range.create(minScore, maxScore), Limit.create(0, 1))
                           .stream()
                           .findFirst();
        }
    }
}
