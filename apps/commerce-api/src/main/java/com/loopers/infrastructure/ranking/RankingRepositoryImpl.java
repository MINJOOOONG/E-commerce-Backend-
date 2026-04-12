package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Component
public class RankingRepositoryImpl implements RankingRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String DAILY_SQL = """
            SELECT product_id, score
            FROM product_metrics
            WHERE metric_date = ?
            ORDER BY score DESC
            LIMIT ?
            """;

    private static final String WEEKLY_SQL = """
            SELECT product_id, total_score, ranking
            FROM mv_product_rank_weekly
            WHERE base_date = ?
            ORDER BY ranking ASC
            LIMIT ?
            """;

    private static final String MONTHLY_SQL = """
            SELECT product_id, total_score, ranking
            FROM mv_product_rank_monthly
            WHERE base_date = ?
            ORDER BY ranking ASC
            LIMIT ?
            """;

    @Override
    public List<RankingEntry> findDailyRanking(LocalDate date, int size) {
        AtomicInteger rank = new AtomicInteger(0);
        return jdbcTemplate.query(DAILY_SQL, dailyRowMapper(rank), date, size);
    }

    @Override
    public List<RankingEntry> findWeeklyRanking(LocalDate baseDate, int size) {
        return jdbcTemplate.query(WEEKLY_SQL, mvRowMapper(), baseDate, size);
    }

    @Override
    public List<RankingEntry> findMonthlyRanking(LocalDate baseDate, int size) {
        return jdbcTemplate.query(MONTHLY_SQL, mvRowMapper(), baseDate, size);
    }

    private RowMapper<RankingEntry> dailyRowMapper(AtomicInteger rank) {
        return (rs, rowNum) -> new RankingEntry(
                rs.getLong("product_id"),
                rs.getDouble("score"),
                rank.incrementAndGet()
        );
    }

    private RowMapper<RankingEntry> mvRowMapper() {
        return (rs, rowNum) -> new RankingEntry(
                rs.getLong("product_id"),
                rs.getDouble("total_score"),
                rs.getInt("ranking")
        );
    }
}
