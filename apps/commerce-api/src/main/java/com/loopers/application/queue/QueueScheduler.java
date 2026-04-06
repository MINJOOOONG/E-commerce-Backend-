package com.loopers.application.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 대기열에서 사용자를 꺼내 입장 토큰을 발급하는 스케줄러.
 *
 * fixedDelay = 1000ms (1초):
 * - 이전 실행이 끝난 후 1초 뒤에 다음 실행
 * - 배치 크기 10명 × 매초 → 초당 최대 10명 입장
 * - Thundering Herd 방지: 짧은 주기 + 작은 배치로 부하 평탄화
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class QueueScheduler {

    private final QueueFacade queueFacade;

    @Scheduled(fixedDelay = 1000)
    public void scheduleQueueProcessing() {
        try {
            queueFacade.processQueue();
        } catch (Exception e) {
            log.error("[QueueScheduler] 대기열 처리 중 오류 발생", e);
        }
    }
}
