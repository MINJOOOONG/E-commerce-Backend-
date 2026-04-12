package com.loopers.domain.productmetrics;

import java.time.LocalDate;
import java.util.List;

public interface ProductMetricsRepository {

    ProductMetrics save(ProductMetrics productMetrics);

    List<ProductMetrics> saveAll(List<ProductMetrics> productMetricsList);

    List<ProductMetrics> findByMetricDate(LocalDate metricDate);

    void deleteByMetricDate(LocalDate metricDate);
}
