package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class UserCouponRepositoryImpl implements UserCouponRepository {

    private final UserCouponJpaRepository jpaRepository;

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        return jpaRepository.save(userCoupon);
    }

    @Override
    public boolean existsByUserIdAndCouponTemplateId(Long userId, Long couponTemplateId) {
        return jpaRepository.existsByUserIdAndCouponTemplateId(userId, couponTemplateId);
    }

    @Override
    public Optional<UserCoupon> findByIdWithLock(Long id) {
        return jpaRepository.findByIdWithLock(id);
    }
}
