package com.loopers.infrastructure.productrank;

import com.loopers.domain.productrank.MonthlyProductRank;
import com.loopers.domain.productrank.MonthlyProductRankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
public class MonthlyProductRankRepositoryImpl implements MonthlyProductRankRepository {

    private final MonthlyProductRankJpaRepository monthlyProductRankJpaRepository;

    @Override
    public List<MonthlyProductRank> saveAll(List<MonthlyProductRank> ranks) {
        return monthlyProductRankJpaRepository.saveAll(ranks);
    }

    @Override
    public void deleteByBaseDate(LocalDate baseDate) {
        monthlyProductRankJpaRepository.deleteByBaseDate(baseDate);
    }
}
