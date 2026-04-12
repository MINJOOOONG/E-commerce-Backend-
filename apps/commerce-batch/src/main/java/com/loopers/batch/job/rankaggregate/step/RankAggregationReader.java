package com.loopers.batch.job.rankaggregate.step;

import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.time.LocalDate;

public class RankAggregationReader {

    private static final String AGGREGATION_SQL = """
            SELECT pm.product_id, SUM(pm.score) as total_score
            FROM product_metrics pm
            WHERE pm.metric_date BETWEEN ? AND ?
            GROUP BY pm.product_id
            ORDER BY total_score DESC
            LIMIT 100
            """;

    private static final RowMapper<ProductScoreAggregation> ROW_MAPPER = (rs, rowNum) ->
            new ProductScoreAggregation(
                    rs.getLong("product_id"),
                    rs.getDouble("total_score")
            );

    private RankAggregationReader() {}

    public static JdbcCursorItemReader<ProductScoreAggregation> create(
            String name,
            DataSource dataSource,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new JdbcCursorItemReaderBuilder<ProductScoreAggregation>()
                .name(name)
                .dataSource(dataSource)
                .sql(AGGREGATION_SQL)
                .rowMapper(ROW_MAPPER)
                .preparedStatementSetter(ps -> {
                    ps.setObject(1, startDate);
                    ps.setObject(2, endDate);
                })
                .build();
    }
}
