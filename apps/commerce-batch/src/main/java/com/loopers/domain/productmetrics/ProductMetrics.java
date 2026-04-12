package com.loopers.domain.productmetrics;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "product_metrics",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_metrics",
                columnNames = {"product_id", "metric_date"}
        ),
        indexes = @Index(name = "idx_metric_date", columnList = "metric_date")
)
public class ProductMetrics extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "score", nullable = false)
    private double score;

    protected ProductMetrics() {}

    public ProductMetrics(Long productId, LocalDate metricDate, double score) {
        this.productId = productId;
        this.metricDate = metricDate;
        this.score = score;
    }
}
