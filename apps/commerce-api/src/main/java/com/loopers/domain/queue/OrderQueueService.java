package com.loopers.domain.queue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderQueueService {

    boolean enqueue(Long userId);

    Optional<Long> getPosition(Long userId);

    long getWaitingCount();

    List<Long> dequeue(int count);

    void issueToken(Long userId, String token, long ttlSeconds);

    /**
     * 대기열에서 앞의 count명을 꺼내고, 각 userId에 대해 토큰을 원자적으로 발급한다.
     * @return 발급된 userId → token 매핑 (빈 대기열이면 빈 Map)
     */
    Map<Long, String> dequeueAndIssueTokens(int count, long ttlSeconds);

    Optional<String> getToken(Long userId);

    void removeToken(Long userId);
}
