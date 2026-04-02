package com.loopers.application.queue;

import com.loopers.domain.queue.OrderQueueService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class QueueFacade {

    private final OrderQueueService orderQueueService;

    /**
     * 시스템 처리량(TPS) 기준 상수.
     *
     * 산정 근거:
     * - HikariCP max pool size: 40
     * - 주문 1건 평균 처리 시간: ~200ms (DB lock + 재고차감 + 쿠폰 + 포인트)
     * - 안전 TPS = pool size / 평균처리시간 = 40 / 0.2 = 200 TPS (이론치)
     * - 안전 마진 50% 적용 → 실질 TPS ≈ 100
     * - 스케줄러 주기 1초, 배치 크기 10명 → 초당 10명 입장
     * - Thundering Herd 방지를 위해 보수적으로 설정
     *   (한 번에 100명 토큰 발급 → 동시 주문 100건 → DB 부하 폭증)
     *   (한 번에 10명 × 매초 반복 → 부하 분산 + 평탄화)
     */
    static final int BATCH_SIZE = 10;
    static final long TOKEN_TTL_SECONDS = 300L;
    static final int ESTIMATED_TPS = 10;

    public QueueInfo.EnterResult enter(Long userId) {
        orderQueueService.enqueue(userId);

        Optional<Long> position = orderQueueService.getPosition(userId);
        long totalWaiting = orderQueueService.getWaitingCount();

        return new QueueInfo.EnterResult(
            userId,
            position.orElse(0L),
            totalWaiting
        );
    }

    public QueueInfo.PositionResult getPosition(Long userId) {
        // 먼저 토큰이 있는지 확인 (이미 입장 가능한 상태)
        Optional<String> token = orderQueueService.getToken(userId);
        if (token.isPresent()) {
            return new QueueInfo.PositionResult(userId, 0, 0, 0, token.get());
        }

        // 대기열에서 순번 확인
        Optional<Long> position = orderQueueService.getPosition(userId);
        if (position.isEmpty()) {
            throw new CoreException(ErrorType.NOT_FOUND, "대기열에 등록되지 않은 사용자입니다.");
        }

        long totalWaiting = orderQueueService.getWaitingCount();
        long estimatedWaitSeconds = (long) Math.ceil((double) position.get() / BATCH_SIZE);

        return new QueueInfo.PositionResult(
            userId,
            position.get(),
            totalWaiting,
            estimatedWaitSeconds,
            null
        );
    }

    public void processQueue() {
        List<Long> userIds = orderQueueService.dequeue(BATCH_SIZE);
        if (userIds.isEmpty()) {
            return;
        }

        for (Long userId : userIds) {
            String token = UUID.randomUUID().toString();
            orderQueueService.issueToken(userId, token, TOKEN_TTL_SECONDS);
            log.info("[Queue] 입장 토큰 발급 - userId={}, token={}", userId, token);
        }

        log.info("[Queue] 배치 처리 완료 - {}명 입장 허용", userIds.size());
    }

    public void validateToken(Long userId) {
        Optional<String> token = orderQueueService.getToken(userId);
        if (token.isEmpty()) {
            throw new CoreException(ErrorType.FORBIDDEN, "입장 토큰이 없거나 만료되었습니다. 대기열을 통해 입장해주세요.");
        }
    }

    public void consumeToken(Long userId) {
        orderQueueService.removeToken(userId);
    }
}
