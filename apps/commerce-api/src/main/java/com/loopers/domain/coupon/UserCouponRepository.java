package com.loopers.domain.coupon;

import java.util.Optional;

public interface UserCouponRepository {

    UserCoupon save(UserCoupon userCoupon);

    boolean existsByUserIdAndCouponTemplateId(Long userId, Long couponTemplateId);

    Optional<UserCoupon> findByIdWithLock(Long id);
}
