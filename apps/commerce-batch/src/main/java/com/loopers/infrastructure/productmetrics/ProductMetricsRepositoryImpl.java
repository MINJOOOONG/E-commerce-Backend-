package com.loopers.infrastructure.productmetrics;

import com.loopers.domain.productmetrics.ProductMetrics;
import com.loopers.domain.productmetrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {

    private final ProductMetricsJpaRepository productMetricsJpaRepository;

    @Override
    public ProductMetrics save(ProductMetrics productMetrics) {
        return productMetricsJpaRepository.save(productMetrics);
    }

    @Override
    public List<ProductMetrics> saveAll(List<ProductMetrics> productMetricsList) {
        return productMetricsJpaRepository.saveAll(productMetricsList);
    }

    @Override
    public List<ProductMetrics> findByMetricDate(LocalDate metricDate) {
        return productMetricsJpaRepository.findByMetricDate(metricDate);
    }

    @Override
    public void deleteByMetricDate(LocalDate metricDate) {
        productMetricsJpaRepository.deleteByMetricDate(metricDate);
    }
}
