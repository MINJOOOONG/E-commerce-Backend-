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
import java.util.ArrayList;
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
        validateRequestDate();

        String dateKey = requestDate.format(DATE_FORMAT);
        log.info("일간 스냅샷 배치 시작 - requestDate: {}, dateKey: {}", requestDate, dateKey);

        List<ProductScore> scores = rankingSnapshotRepository.getAllScores(dateKey);
        log.info("Redis에서 조회된 항목 수: {}", scores.size());

        productMetricsRepository.deleteByMetricDate(requestDate);

        if (!scores.isEmpty()) {
            List<ProductMetrics> metricsList = new ArrayList<>();
            int skipCount = 0;

            for (ProductScore ps : scores) {
                if (ps.productId() == null) {
                    log.warn("productId가 null인 항목 skip - score: {}", ps.score());
                    skipCount++;
                    continue;
                }
                metricsList.add(new ProductMetrics(ps.productId(), requestDate, ps.score()));
            }

            if (!metricsList.isEmpty()) {
                productMetricsRepository.saveAll(metricsList);
            }

            contribution.incrementWriteCount(metricsList.size());
            log.info("product_metrics 적재 완료 - 적재: {} 건, skip: {} 건", metricsList.size(), skipCount);
        }

        return RepeatStatus.FINISHED;
    }

    private void validateRequestDate() {
        if (requestDate == null) {
            throw new RuntimeException("requestDate is required");
        }
        if (requestDate.isAfter(LocalDate.now())) {
            throw new RuntimeException("requestDate는 미래 날짜일 수 없습니다: " + requestDate);
        }
    }
}
