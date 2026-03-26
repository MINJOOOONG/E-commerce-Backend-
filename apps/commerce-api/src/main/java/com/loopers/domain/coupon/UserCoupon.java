package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_coupons", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "coupon_template_id"})
})
public class UserCoupon extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_template_id", nullable = false)
    private Long couponTemplateId;

    public UserCoupon(Long userId, Long couponTemplateId) {
        this.userId = userId;
        this.couponTemplateId = couponTemplateId;
    }
}
