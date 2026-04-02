package com.loopers.domain.queue;

import java.util.List;
import java.util.Optional;

public interface OrderQueueService {

    boolean enqueue(Long userId);

    Optional<Long> getPosition(Long userId);

    long getWaitingCount();

    List<Long> dequeue(int count);

    void issueToken(Long userId, String token, long ttlSeconds);

    Optional<String> getToken(Long userId);

    void removeToken(Long userId);
}
