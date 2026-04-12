package com.loopers.domain.productrank;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "mv_product_rank_weekly",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_weekly_rank",
                columnNames = {"base_date", "product_id"}
        ),
        indexes = @Index(name = "idx_weekly_base_date", columnList = "base_date")
)
public class WeeklyProductRank extends BaseProductRank {

    protected WeeklyProductRank() {}

    public WeeklyProductRank(Long productId, double totalScore, int ranking, LocalDate baseDate) {
        super(productId, totalScore, ranking, baseDate);
    }
}
