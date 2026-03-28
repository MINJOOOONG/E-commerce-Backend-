package com.loopers.domain.coupon;

public interface UserCouponRepository {

    UserCoupon save(UserCoupon userCoupon);

    boolean existsByUserIdAndCouponTemplateId(Long userId, Long couponTemplateId);
}
