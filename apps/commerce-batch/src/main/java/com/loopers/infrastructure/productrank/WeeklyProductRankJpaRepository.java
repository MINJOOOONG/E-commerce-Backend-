package com.loopers.infrastructure.productrank;

import com.loopers.domain.productrank.WeeklyProductRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface WeeklyProductRankJpaRepository extends JpaRepository<WeeklyProductRank, Long> {

    @Modifying
    @Query("DELETE FROM WeeklyProductRank r WHERE r.baseDate = :baseDate")
    void deleteByBaseDate(@Param("baseDate") LocalDate baseDate);
}
