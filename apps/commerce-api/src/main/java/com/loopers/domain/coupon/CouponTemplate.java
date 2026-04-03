package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupon_templates")
public class CouponTemplate extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "issued_count", nullable = false)
    private int issuedCount;

    public CouponTemplate(String name, int totalQuantity) {
        if (totalQuantity < 1) {
            throw new CoreException(ErrorType.BAD_REQUEST, "총 수량은 1 이상이어야 합니다");
        }
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.issuedCount = 0;
    }

    // TODO: 실제 할인 정책 미구현 — 할인 필드(정액/정률) 확정 후 구현 필요
    public long calculateDiscount(long totalPrice) {
        return 0L;
    }

    public void issue() {
        if (this.issuedCount >= this.totalQuantity) {
            throw new CoreException(ErrorType.CONFLICT, "쿠폰이 모두 소진되었습니다");
        }
        this.issuedCount++;
    }
}
