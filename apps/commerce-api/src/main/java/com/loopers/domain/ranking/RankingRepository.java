package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.util.List;

public interface RankingRepository {

    List<RankingEntry> findDailyRanking(LocalDate date, int size);

    List<RankingEntry> findWeeklyRanking(LocalDate baseDate, int size);

    List<RankingEntry> findMonthlyRanking(LocalDate baseDate, int size);

    record RankingEntry(Long productId, double score, int ranking) {}
}
