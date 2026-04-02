package com.loopers.application.queue;

import com.loopers.domain.queue.OrderQueueService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FakeOrderQueueService implements OrderQueueService {

    private final LinkedHashMap<Long, Double> queue = new LinkedHashMap<>();
    private final Map<Long, String> tokens = new ConcurrentHashMap<>();
    private final AtomicLong scoreCounter = new AtomicLong(0);

    @Override
    public boolean enqueue(Long userId) {
        if (queue.containsKey(userId)) {
            return false;
        }
        queue.put(userId, (double) scoreCounter.incrementAndGet());
        return true;
    }

    @Override
    public Optional<Long> getPosition(Long userId) {
        if (!queue.containsKey(userId)) {
            return Optional.empty();
        }
        long position = 0;
        for (Long key : queue.keySet()) {
            position++;
            if (key.equals(userId)) {
                return Optional.of(position);
            }
        }
        return Optional.empty();
    }

    @Override
    public long getWaitingCount() {
        return queue.size();
    }

    @Override
    public List<Long> dequeue(int count) {
        List<Long> result = new ArrayList<>();
        var iterator = queue.entrySet().iterator();
        int i = 0;
        while (iterator.hasNext() && i < count) {
            result.add(iterator.next().getKey());
            iterator.remove();
            i++;
        }
        return result;
    }

    @Override
    public void issueToken(Long userId, String token, long ttlSeconds) {
        tokens.put(userId, token);
    }

    @Override
    public Optional<String> getToken(Long userId) {
        return Optional.ofNullable(tokens.get(userId));
    }

    @Override
    public void removeToken(Long userId) {
        tokens.remove(userId);
    }
}
