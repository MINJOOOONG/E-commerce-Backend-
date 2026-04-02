package com.loopers.infrastructure.queue;

import com.loopers.domain.queue.OrderQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisOrderQueueService implements OrderQueueService {

    private static final String QUEUE_KEY = "order-queue:waiting";
    private static final String TOKEN_KEY_PREFIX = "entry-token:";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisOrderQueueService(
        @Qualifier("redisTemplateMaster") RedisTemplate<String, String> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean enqueue(Long userId) {
        String member = userId.toString();
        double score = System.currentTimeMillis();
        // ZADD NX: member가 이미 존재하면 추가하지 않음 (중복 진입 방지)
        Boolean added = redisTemplate.opsForZSet().addIfAbsent(QUEUE_KEY, member, score);
        return Boolean.TRUE.equals(added);
    }

    @Override
    public Optional<Long> getPosition(Long userId) {
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, userId.toString());
        if (rank == null) {
            return Optional.empty();
        }
        // rank는 0-based이므로 +1하여 1-based 순번 반환
        return Optional.of(rank + 1);
    }

    @Override
    public long getWaitingCount() {
        Long size = redisTemplate.opsForZSet().zCard(QUEUE_KEY);
        return size != null ? size : 0L;
    }

    @Override
    public List<Long> dequeue(int count) {
        if (count <= 0) {
            return List.of();
        }
        // ZPOPMIN: score가 가장 낮은(먼저 진입한) N명을 꺼냄
        Set<ZSetOperations.TypedTuple<String>> popped =
            redisTemplate.opsForZSet().popMin(QUEUE_KEY, count);

        if (popped == null || popped.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : popped) {
            if (tuple.getValue() != null) {
                userIds.add(Long.parseLong(tuple.getValue()));
            }
        }
        return userIds;
    }

    @Override
    public void issueToken(Long userId, String token, long ttlSeconds) {
        String key = TOKEN_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, token, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<String> getToken(Long userId) {
        String key = TOKEN_KEY_PREFIX + userId;
        String token = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(token);
    }

    @Override
    public void removeToken(Long userId) {
        String key = TOKEN_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }
}
