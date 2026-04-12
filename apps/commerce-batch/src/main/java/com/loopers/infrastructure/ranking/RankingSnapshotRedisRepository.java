package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingSnapshotRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class RankingSnapshotRedisRepository implements RankingSnapshotRepository {

    private static final String KEY_PREFIX = "ranking:all:";

    private final RedisTemplate<String, String> redisTemplate;

    public RankingSnapshotRedisRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<ProductScore> getAllScores(String dateKey) {
        String key = KEY_PREFIX + dateKey;
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);

        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        List<ProductScore> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long productId = Long.valueOf(tuple.getValue());
            double score = tuple.getScore();
            result.add(new ProductScore(productId, score));
        }
        return result;
    }
}
