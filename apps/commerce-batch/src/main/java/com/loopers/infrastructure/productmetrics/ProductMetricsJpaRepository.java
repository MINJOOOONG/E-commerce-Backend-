package com.loopers.infrastructure.productmetrics;

import com.loopers.domain.productmetrics.ProductMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {

    List<ProductMetrics> findByMetricDate(LocalDate metricDate);

    @Modifying
    @Query("DELETE FROM ProductMetrics pm WHERE pm.metricDate = :metricDate")
    void deleteByMetricDate(@Param("metricDate") LocalDate metricDate);
}
