package com.loopers.domain.ranking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
public class RankingService {

    private final RankingRepository rankingRepository;

    @Transactional(readOnly = true)
    public List<RankingRepository.RankingEntry> getRanking(RankPeriod period, LocalDate date, int size) {
        return switch (period) {
            case DAILY -> rankingRepository.findDailyRanking(date, size);
            case WEEKLY -> rankingRepository.findWeeklyRanking(date, size);
            case MONTHLY -> rankingRepository.findMonthlyRanking(date, size);
        };
    }
}
