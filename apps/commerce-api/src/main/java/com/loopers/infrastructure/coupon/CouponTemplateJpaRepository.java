package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponTemplate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponTemplateJpaRepository extends JpaRepository<CouponTemplate, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ct FROM CouponTemplate ct WHERE ct.id = :id")
    Optional<CouponTemplate> findByIdWithLock(@Param("id") Long id);
}
