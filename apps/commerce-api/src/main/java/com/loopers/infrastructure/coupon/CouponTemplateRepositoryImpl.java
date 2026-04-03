package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class CouponTemplateRepositoryImpl implements CouponTemplateRepository {

    private final CouponTemplateJpaRepository jpaRepository;

    @Override
    public CouponTemplate save(CouponTemplate couponTemplate) {
        return jpaRepository.save(couponTemplate);
    }

    @Override
    public Optional<CouponTemplate> findByIdWithLock(Long id) {
        return jpaRepository.findByIdWithLock(id);
    }

    @Override
    public Optional<CouponTemplate> findById(Long id) {
        return jpaRepository.findById(id);
    }
}
