package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupon_issue_requests")
public class CouponIssueRequest extends BaseEntity {

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_template_id", nullable = false)
    private Long couponTemplateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CouponIssueStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    public CouponIssueRequest(Long userId, Long couponTemplateId) {
        this.requestId = UUID.randomUUID().toString();
        this.userId = userId;
        this.couponTemplateId = couponTemplateId;
        this.status = CouponIssueStatus.PENDING;
    }

    public void succeed() {
        this.status = CouponIssueStatus.SUCCESS;
    }

    public void fail(String reason) {
        this.status = CouponIssueStatus.FAILED;
        this.failureReason = reason;
    }
}
