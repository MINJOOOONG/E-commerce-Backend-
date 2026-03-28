package com.loopers.application.order.event;

import lombok.Getter;

/**
 * 주문 생성 완료 후 발행되는 이벤트.
 *
 * <p>현재는 부가 로직(로그 기록)만 트리거하며,
 * 핵심 도메인 상태(재고, 쿠폰, 포인트)는 변경하지 않는다.</p>
 *
 * <p>OrderFacade에서 이벤트를 발행하려면 아래 코드를 추가:</p>
 * <pre>
 * // 1. 필드 추가
 * private final ApplicationEventPublisher eventPublisher;
 *
 * // 2. createOrder() 마지막, return 직전에 추가
 * eventPublisher.publishEvent(new OrderCreatedEvent(
 *     order.getId(), userId, order.getTotalAmount(), couponId
 * ));
 * </pre>
 */
@Getter
public class OrderCreatedEvent {

    private final Long orderId;
    private final Long userId;
    private final Long totalAmount;
    private final Long couponId;

    public OrderCreatedEvent(Long orderId, Long userId, Long totalAmount) {
        this(orderId, userId, totalAmount, null);
    }

    public OrderCreatedEvent(Long orderId, Long userId, Long totalAmount, Long couponId) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.couponId = couponId;
    }
}
