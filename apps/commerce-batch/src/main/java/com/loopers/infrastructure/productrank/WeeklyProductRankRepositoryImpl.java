package com.loopers.infrastructure.productrank;

import com.loopers.domain.productrank.WeeklyProductRank;
import com.loopers.domain.productrank.WeeklyProductRankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
public class WeeklyProductRankRepositoryImpl implements WeeklyProductRankRepository {

    private final WeeklyProductRankJpaRepository weeklyProductRankJpaRepository;

    @Override
    public List<WeeklyProductRank> saveAll(List<WeeklyProductRank> ranks) {
        return weeklyProductRankJpaRepository.saveAll(ranks);
    }

    @Override
    public void deleteByBaseDate(LocalDate baseDate) {
        weeklyProductRankJpaRepository.deleteByBaseDate(baseDate);
    }
}
