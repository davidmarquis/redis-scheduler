package com.github.davidmarquis.redisscheduler.drivers.spring;

import com.github.davidmarquis.redisscheduler.RedisConnectException;
import com.github.davidmarquis.redisscheduler.RedisDriver;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;

import java.util.Optional;
import java.util.function.Function;

/**
 * Driver using Spring Data Redis
 */
public class RedisTemplateDriver implements RedisDriver {

    private RedisTemplate<String, String> redisTemplate;

    public RedisTemplateDriver(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public <T> T fetch(Function<Commands, T> block) {
        try {
            return redisTemplate.execute(new SessionCallback<T>() {
                @Override
                @SuppressWarnings("unchecked")
                public <K, V> T execute(RedisOperations<K, V> operations) throws DataAccessException {
                    RedisConnectionCommands commands = new RedisConnectionCommands((RedisOperations<String, String>) operations);
                    return block.apply(commands);
                }
            });
        } catch (RedisConnectionFailureException e) {
            throw new RedisConnectException(e);
        }
    }

    private static class RedisConnectionCommands implements Commands {
        private RedisOperations<String, String> ops;

        private RedisConnectionCommands(RedisOperations<String, String> ops) {
            this.ops = ops;
        }

        @Override
        public void addToSetWithScore(String key, String taskId, long score) {
            ops.opsForZSet().add(key, taskId, score);
        }

        @Override
        public void removeFromSet(String key, String taskId) {
            ops.opsForZSet().remove(key, taskId);
        }

        @Override
        public void remove(String key) {
            ops.delete(key);
        }

        @Override
        public void watch(String key) {
            ops.watch(key);
        }

        @Override
        public void unwatch() {
            ops.unwatch();
        }

        @Override
        public void multi() {
            ops.multi();
        }

        @Override
        public boolean exec() {
            return ops.exec() != null;
        }

        @Override
        public Optional<String> firstByScore(String key, long minScore, long maxScore) {
            return ops.opsForZSet()
                      .rangeByScore(key, minScore, maxScore, 0, 1)
                      .stream()
                      .findFirst();
        }
    }
}
