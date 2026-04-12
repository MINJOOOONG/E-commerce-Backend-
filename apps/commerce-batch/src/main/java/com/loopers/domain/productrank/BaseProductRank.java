package com.loopers.domain.productrank;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@MappedSuperclass
public abstract class BaseProductRank extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "total_score", nullable = false)
    private double totalScore;

    @Column(name = "ranking", nullable = false)
    private int ranking;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    protected BaseProductRank() {}

    protected BaseProductRank(Long productId, double totalScore, int ranking, LocalDate baseDate) {
        this.productId = productId;
        this.totalScore = totalScore;
        this.ranking = ranking;
        this.baseDate = baseDate;
        guard();
    }

    @Override
    protected void guard() {
        if (productId == null) {
            throw new IllegalArgumentException("productId는 null일 수 없습니다");
        }
        if (totalScore < 0) {
            throw new IllegalArgumentException("totalScore는 0 이상이어야 합니다");
        }
        if (ranking < 1) {
            throw new IllegalArgumentException("ranking은 1 이상이어야 합니다");
        }
        if (baseDate == null) {
            throw new IllegalArgumentException("baseDate는 null일 수 없습니다");
        }
    }
}
