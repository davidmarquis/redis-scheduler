package com.github.davidmarquis.redisscheduler.drivers.jedis;

import com.github.davidmarquis.redisscheduler.RedisConnectException;
import com.github.davidmarquis.redisscheduler.RedisDriver;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public class JedisDriver implements RedisDriver {

    private JedisPool jedisPool;

    public JedisDriver(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public <T> T fetch(Function<Commands, T> block) {
        try (Jedis jedis = jedisPool.getResource()) {
            return block.apply(new JedisCommands(jedis));
        }
    }

    private static class JedisCommands implements Commands {

        private Jedis jedis;

        private Transaction txn;

        private JedisCommands(Jedis jedis) {
            this.jedis = jedis;
        }

        @Override
        public void addToSetWithScore(String key, String taskId, long score) {
            jedis.zadd(key, score, taskId);
        }

        @Override
        public void removeFromSet(String key, String taskId) {
            if (txn != null) {
                txn.zrem(key, taskId);
            } else {
                jedis.zrem(key, taskId);
            }
        }

        @Override
        public void remove(String key) {
            jedis.del(key);
        }

        @Override
        public void watch(String key) {
            jedis.watch(key);
        }

        @Override
        public void unwatch() {
            jedis.unwatch();
        }

        @Override
        public void multi() {
            txn = jedis.multi();
        }

        @Override
        public boolean exec() {
            try {
                return Optional.ofNullable(txn)
                               .map(Transaction::exec)
                               .map(col -> !col.isEmpty())
                               .orElse(false);
            } finally {
                try {
                    txn.close();
                } catch (Exception e) {
                    throw new RedisConnectException(e);
                }
                txn = null;
            }
        }

        @Override
        public Optional<String> firstByScore(String key, long minScore, long maxScore) {
            return jedis.zrangeByScore(key, minScore, maxScore, 0, 1)
                        .stream()
                        .findFirst();
        }
    }
}
