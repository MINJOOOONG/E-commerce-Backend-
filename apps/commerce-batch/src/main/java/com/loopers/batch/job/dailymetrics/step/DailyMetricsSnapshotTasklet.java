package com.loopers.batch.job.dailymetrics.step;

import com.loopers.batch.job.dailymetrics.DailyMetricsSnapshotJobConfig;
import com.loopers.domain.productmetrics.ProductMetrics;
import com.loopers.domain.productmetrics.ProductMetricsRepository;
import com.loopers.domain.ranking.RankingSnapshotRepository;
import com.loopers.domain.ranking.RankingSnapshotRepository.ProductScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@StepScope
@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = DailyMetricsSnapshotJobConfig.JOB_NAME)
@RequiredArgsConstructor
@Component
public class DailyMetricsSnapshotTasklet implements Tasklet {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("#{jobParameters['requestDate']}")
    private LocalDate requestDate;

    private final RankingSnapshotRepository rankingSnapshotRepository;
    private final ProductMetricsRepository productMetricsRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        if (requestDate == null) {
            throw new RuntimeException("requestDate is required");
        }

        String dateKey = requestDate.format(DATE_FORMAT);
        log.info("일간 스냅샷 배치 시작 - requestDate: {}, dateKey: {}", requestDate, dateKey);

        List<ProductScore> scores = rankingSnapshotRepository.getAllScores(dateKey);
        log.info("Redis에서 조회된 상품 수: {}", scores.size());

        productMetricsRepository.deleteByMetricDate(requestDate);

        if (!scores.isEmpty()) {
            List<ProductMetrics> metricsList = scores.stream()
                    .map(ps -> new ProductMetrics(ps.productId(), requestDate, ps.score()))
                    .toList();
            productMetricsRepository.saveAll(metricsList);
            log.info("product_metrics 적재 완료 - {} 건", metricsList.size());
        }

        return RepeatStatus.FINISHED;
    }
}
