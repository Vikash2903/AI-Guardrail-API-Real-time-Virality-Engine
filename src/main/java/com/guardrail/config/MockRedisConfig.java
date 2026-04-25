package com.guardrail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.connection.RedisListCommands.Direction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
public class MockRedisConfig {

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return new MockStringRedisTemplate();
    }

    private static class MockStringRedisTemplate extends StringRedisTemplate {
        private final Map<String, String> values = new ConcurrentHashMap<>();
        private final Map<String, List<String>> lists = new ConcurrentHashMap<>();
        private final Map<String, Long> expirations = new ConcurrentHashMap<>();

        @Override
        public ValueOperations<String, String> opsForValue() {
            return new MockValueOperations(values, expirations);
        }

        @Override
        public ListOperations<String, String> opsForList() {
            return new MockListOperations(lists);
        }

        @Override
        public Boolean hasKey(String key) {
            checkExpiry(key);
            return values.containsKey(key) || lists.containsKey(key);
        }

        @Override
        public Boolean delete(String key) {
            return values.remove(key) != null || lists.remove(key) != null;
        }
        
        @Override
        public Long delete(Collection<String> keys) {
            return keys.stream().map(this::delete).filter(b -> b).count();
        }

        @Override
        public Set<String> keys(String pattern) {
            String regex = pattern.replace("*", ".*");
            return values.keySet().stream()
                    .filter(k -> k.matches(regex))
                    .collect(Collectors.toSet());
        }

        private void checkExpiry(String key) {
            Long expiry = expirations.get(key);
            if (expiry != null && System.currentTimeMillis() > expiry) {
                values.remove(key);
                expirations.remove(key);
            }
        }
        
        @Override
        public void setConnectionFactory(RedisConnectionFactory connectionFactory) {
            // Do nothing
        }
        
        @Override
        public void afterPropertiesSet() {
            // Do not call super to avoid connection factory check
        }
    }

    private static class MockValueOperations implements ValueOperations<String, String> {
        private final Map<String, String> values;
        private final Map<String, Long> expirations;

        MockValueOperations(Map<String, String> values, Map<String, Long> expirations) {
            this.values = values;
            this.expirations = expirations;
        }

        @Override public void set(String key, String value) { values.put(key, value); }
        @Override public void set(String key, String value, long timeout, TimeUnit unit) {
            values.put(key, value);
            expirations.put(key, System.currentTimeMillis() + unit.toMillis(timeout));
        }
        @Override public Boolean setIfAbsent(String key, String value) { return values.putIfAbsent(key, value) == null; }
        @Override public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
            boolean absent = values.putIfAbsent(key, value) == null;
            if (absent) expirations.put(key, System.currentTimeMillis() + unit.toMillis(timeout));
            return absent;
        }
        @Override public String get(Object key) { return values.get((String)key); }
        @Override public Long increment(String key) {
            String val = values.getOrDefault(key, "0");
            long next = Long.parseLong(val) + 1;
            values.put(key, String.valueOf(next));
            return next;
        }
        @Override public Long increment(String key, long delta) {
            String val = values.getOrDefault(key, "0");
            long next = Long.parseLong(val) + delta;
            values.put(key, String.valueOf(next));
            return next;
        }
        @Override public Long decrement(String key) { return increment(key, -1); }
        @Override public Long decrement(String key, long delta) { return increment(key, -delta); }

        @Override public void set(String key, String value, java.time.Duration timeout) { set(key, value, timeout.toMillis(), TimeUnit.MILLISECONDS); }
        @Override public Boolean setIfAbsent(String key, String value, java.time.Duration timeout) { return setIfAbsent(key, value, timeout.toMillis(), TimeUnit.MILLISECONDS); }

        @Override public Boolean setIfPresent(String key, String value) { return values.containsKey(key); }
        @Override public Boolean setIfPresent(String key, String value, long timeout, TimeUnit unit) { return values.containsKey(key); }
        @Override public Boolean setIfPresent(String key, String value, java.time.Duration timeout) { return values.containsKey(key); }
        @Override public String getAndDelete(String key) { return values.remove(key); }
        @Override public String getAndExpire(String key, long timeout, TimeUnit unit) { return values.get(key); }
        @Override public String getAndExpire(String key, java.time.Duration timeout) { return values.get(key); }
        @Override public String getAndPersist(String key) { return values.get(key); }
        @Override public String getAndSet(String key, String value) { return values.put(key, value); }
        @Override public List<String> multiGet(Collection<String> keys) { return keys.stream().map(k -> values.get(k)).collect(Collectors.toList()); }
        @Override public Double increment(String key, double delta) { return 0.0; }
        @Override public Integer append(String key, String value) { return 0; }
        @Override public String get(String key, long start, long end) { return ""; }
        @Override public void set(String key, String value, long offset) {}
        @Override public Long size(String key) { return (long) values.size(); }
        @Override public Boolean setBit(String key, long offset, boolean value) { return false; }
        @Override public Boolean getBit(String key, long offset) { return false; }
        @Override public List<Long> bitField(String key, BitFieldSubCommands subCommands) { return null; }
        @Override public void multiSet(Map<? extends String, ? extends String> map) { values.putAll(map); }
        @Override public Boolean multiSetIfAbsent(Map<? extends String, ? extends String> map) { return false; }
        @Override public RedisOperations<String, String> getOperations() { return null; }
    }

    private static class MockListOperations implements ListOperations<String, String> {
        private final Map<String, List<String>> lists;
        MockListOperations(Map<String, List<String>> lists) { this.lists = lists; }

        @Override public Long rightPush(String key, String value) {
            List<String> list = lists.computeIfAbsent(key, k -> new ArrayList<>());
            list.add(value);
            return (long) list.size();
        }
        @Override public List<String> range(String key, long start, long end) {
            List<String> list = lists.get(key);
            if (list == null) return Collections.emptyList();
            return new ArrayList<>(list);
        }
        @Override public Long size(String key) {
            List<String> list = lists.get(key);
            return list == null ? 0L : (long) list.size();
        }
        @Override public Long indexOf(String key, String value) { return null; }
        @Override public Long lastIndexOf(String key, String value) { return null; }
        @Override public String move(String sourceKey, Direction from, String destinationKey, Direction to, long timeout, TimeUnit unit) { return null; }
        @Override public String move(String sourceKey, Direction from, String destinationKey, Direction to, java.time.Duration timeout) { return null; }
        @Override public String move(String sourceKey, Direction from, String destinationKey, Direction to) { return null; }
        @Override public List<String> leftPop(String key, long count) { return null; }
        @Override public String leftPop(String key) { return null; }
        @Override public String leftPop(String key, long timeout, TimeUnit unit) { return null; }
        @Override public String leftPop(String key, java.time.Duration timeout) { return null; }
        @Override public List<String> rightPop(String key, long count) { return null; }
        @Override public String rightPop(String key) { return null; }
        @Override public String rightPop(String key, long timeout, TimeUnit unit) { return null; }
        @Override public String rightPop(String key, java.time.Duration timeout) { return null; }
        @Override public String rightPopAndLeftPush(String sourceKey, String destinationKey) { return null; }
        @Override public String rightPopAndLeftPush(String sourceKey, String destinationKey, long timeout, TimeUnit unit) { return null; }
        @Override public String rightPopAndLeftPush(String sourceKey, String destinationKey, java.time.Duration timeout) { return null; }
        @Override public Long leftPush(String key, String value) { return 0L; }
        @Override public Long leftPushAll(String key, String... values) { return 0L; }
        @Override public Long leftPushAll(String key, Collection<String> values) { return 0L; }
        @Override public Long leftPushIfPresent(String key, String value) { return 0L; }
        @Override public Long leftPush(String key, String pivot, String value) { return 0L; }
        @Override public Long rightPushAll(String key, String... values) { return 0L; }
        @Override public Long rightPushAll(String key, Collection<String> values) { return 0L; }
        @Override public Long rightPushIfPresent(String key, String value) { return 0L; }
        @Override public Long rightPush(String key, String pivot, String value) { return 0L; }
        @Override public void set(String key, long index, String value) {}
        @Override public Long remove(String key, long count, Object value) { return 0L; }
        @Override public String index(String key, long index) { return null; }
        @Override public void trim(String key, long start, long end) {}
        @Override public RedisOperations<String, String> getOperations() { return null; }
    }
}
