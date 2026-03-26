package com.loopers.domain.outbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {

    ORDER_CREATED("order-events"),
    COUPON_ISSUE_REQUESTED("coupon-issue-requests");

    private final String topic;
}
