package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponIssueRequestInfo;
import com.loopers.domain.coupon.CouponIssueStatus;

public class CouponV1Dto {

    public record IssueResponse(
        String requestId
    ) {}

    public record IssueRequestStatusResponse(
        String requestId,
        CouponIssueStatus status,
        String failureReason
    ) {
        public static IssueRequestStatusResponse from(CouponIssueRequestInfo info) {
            return new IssueRequestStatusResponse(
                info.requestId(),
                info.status(),
                info.failureReason()
            );
        }
    }
}
