package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.ZonedDateTime;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "pg_transaction_id", unique = true)
    private String pgTransactionId;

    @Column(name = "pg_response_code", length = 50)
    private String pgResponseCode;

    @Column(name = "approved_at")
    private ZonedDateTime approvedAt;

    protected Payment() {}

    public Payment(Long orderId, Long userId, Long amount, PaymentMethod method) {
        if (orderId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "orderId는 필수입니다");
        }
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId는 필수입니다");
        }
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다");
        }
        if (method == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 수단은 필수입니다");
        }
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.method = method;
        this.status = PaymentStatus.PENDING;
    }

    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public Long getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public String getPgTransactionId() { return pgTransactionId; }
    public String getPgResponseCode() { return pgResponseCode; }
    public ZonedDateTime getApprovedAt() { return approvedAt; }

    public void assignPgTransactionId(String pgTransactionId) {
        if (pgTransactionId == null || pgTransactionId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "pgTransactionId는 필수입니다");
        }
        this.pgTransactionId = pgTransactionId;
    }

    public void approve(String pgTransactionId, String responseCode) {
        if (this.status != PaymentStatus.PENDING) {
            throw new CoreException(ErrorType.CONFLICT, "결제 대기 상태에서만 승인할 수 있습니다");
        }
        this.status = PaymentStatus.APPROVED;
        this.pgTransactionId = pgTransactionId;
        this.pgResponseCode = responseCode;
        this.approvedAt = ZonedDateTime.now();
    }

    public void fail(String responseCode) {
        if (this.status != PaymentStatus.PENDING) {
            throw new CoreException(ErrorType.CONFLICT, "결제 대기 상태에서만 실패 처리할 수 있습니다");
        }
        this.status = PaymentStatus.FAILED;
        this.pgResponseCode = responseCode;
    }

    public void cancel() {
        if (this.status == PaymentStatus.CANCELLED) {
            return;
        }
        if (this.status != PaymentStatus.APPROVED) {
            throw new CoreException(ErrorType.CONFLICT, "승인된 결제만 취소할 수 있습니다");
        }
        this.status = PaymentStatus.CANCELLED;
    }
}
