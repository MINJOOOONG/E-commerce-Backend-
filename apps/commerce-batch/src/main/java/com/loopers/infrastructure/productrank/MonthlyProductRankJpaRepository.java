package com.loopers.infrastructure.productrank;

import com.loopers.domain.productrank.MonthlyProductRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface MonthlyProductRankJpaRepository extends JpaRepository<MonthlyProductRank, Long> {

    @Modifying
    @Query("DELETE FROM MonthlyProductRank r WHERE r.baseDate = :baseDate")
    void deleteByBaseDate(@Param("baseDate") LocalDate baseDate);
}
