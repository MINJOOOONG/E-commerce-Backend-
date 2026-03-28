package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponIssueRequest;
import com.loopers.domain.coupon.CouponIssueStatus;

public record CouponIssueRequestInfo(
    String requestId,
    CouponIssueStatus status,
    String failureReason
) {
    public static CouponIssueRequestInfo from(CouponIssueRequest request) {
        return new CouponIssueRequestInfo(
            request.getRequestId(),
            request.getStatus(),
            request.getFailureReason()
        );
    }
}
