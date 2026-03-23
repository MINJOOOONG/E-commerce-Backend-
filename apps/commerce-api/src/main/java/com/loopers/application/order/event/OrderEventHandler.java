package com.loopers.application.order.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class OrderEventHandler {

    @Async("orderEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[OrderCreatedEvent] 주문 생성 완료 - orderId={}, userId={}, totalAmount={}, couponId={}",
                event.getOrderId(),
                event.getUserId(),
                event.getTotalAmount(),
                event.getCouponId()
        );

        // TODO: 주문 생성 알림 발송 (이메일, 푸시 등)
        // TODO: 주문 메트릭 집계 (주문 수, 매출액 등)
        // TODO: 외부 시스템 연동 (물류, ERP 등)
    }
}
