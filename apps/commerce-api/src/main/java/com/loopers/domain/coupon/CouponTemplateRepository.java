package com.loopers.domain.coupon;

import java.util.Optional;

public interface CouponTemplateRepository {

    CouponTemplate save(CouponTemplate couponTemplate);

    Optional<CouponTemplate> findByIdWithLock(Long id);

    Optional<CouponTemplate> findById(Long id);
}
