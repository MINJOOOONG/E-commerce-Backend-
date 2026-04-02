package com.loopers.application.queue;

import com.loopers.domain.queue.OrderQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
     */
    static final int BATCH_SIZE = 10;
    static final long TOKEN_TTL_SECONDS = 300L;
    static final int ESTIMATED_TPS = 10;

    public QueueInfo.EnterResult enter(Long userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public QueueInfo.PositionResult getPosition(Long userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void processQueue() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void validateAndConsumeToken(Long userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
