package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankPeriod;
import com.loopers.domain.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
public class RankingFacade {

    private final RankingService rankingService;

    public List<RankingInfo> getRanking(RankPeriod period, LocalDate date, int size) {
        return rankingService.getRanking(period, date, size).stream()
                .map(RankingInfo::from)
                .toList();
    }
}
