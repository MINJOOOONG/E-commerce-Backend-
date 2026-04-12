package com.loopers.batch.job.rankaggregate;

import com.loopers.batch.job.rankaggregate.step.MonthlyRankWriter;
import com.loopers.batch.job.rankaggregate.step.ProductScoreAggregation;
import com.loopers.batch.job.rankaggregate.step.RankAggregationReader;
import com.loopers.batch.job.rankaggregate.step.WeeklyRankWriter;
import com.loopers.batch.listener.JobListener;
import com.loopers.batch.listener.StepMonitorListener;
import com.loopers.domain.productrank.MonthlyProductRankRepository;
import com.loopers.domain.productrank.WeeklyProductRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;

@Slf4j
@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = RankAggregationJobConfig.JOB_NAME)
@RequiredArgsConstructor
@Configuration
public class RankAggregationJobConfig {

    public static final String JOB_NAME = "rankAggregationJob";
    private static final String WEEKLY_STEP_NAME = "weeklyRankStep";
    private static final String MONTHLY_STEP_NAME = "monthlyRankStep";
    private static final int CHUNK_SIZE = 100;
    private static final int WEEKLY_DAYS = 7;
    private static final int MONTHLY_DAYS = 30;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobListener jobListener;
    private final StepMonitorListener stepMonitorListener;
    private final DataSource dataSource;
    private final WeeklyProductRankRepository weeklyProductRankRepository;
    private final MonthlyProductRankRepository monthlyProductRankRepository;

    @Bean(JOB_NAME)
    public Job rankAggregationJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(weeklyRankStep(null))
                .next(monthlyRankStep(null))
                .listener(jobListener)
                .build();
    }

    @JobScope
    @Bean(WEEKLY_STEP_NAME)
    public Step weeklyRankStep(
            @Value("#{jobParameters['requestDate']}") LocalDate requestDate
    ) {
        validateRequestDate(requestDate);
        LocalDate startDate = requestDate.minusDays(WEEKLY_DAYS - 1);

        log.info("주간 랭킹 집계 Step 구성 - 기간: {} ~ {}", startDate, requestDate);

        return new StepBuilder(WEEKLY_STEP_NAME, jobRepository)
                .<ProductScoreAggregation, ProductScoreAggregation>chunk(CHUNK_SIZE, transactionManager)
                .reader(weeklyReader(startDate, requestDate))
                .writer(new WeeklyRankWriter(weeklyProductRankRepository, requestDate))
                .listener(stepMonitorListener)
                .build();
    }

    @JobScope
    @Bean(MONTHLY_STEP_NAME)
    public Step monthlyRankStep(
            @Value("#{jobParameters['requestDate']}") LocalDate requestDate
    ) {
        validateRequestDate(requestDate);
        LocalDate startDate = requestDate.minusDays(MONTHLY_DAYS - 1);

        log.info("월간 랭킹 집계 Step 구성 - 기간: {} ~ {}", startDate, requestDate);

        return new StepBuilder(MONTHLY_STEP_NAME, jobRepository)
                .<ProductScoreAggregation, ProductScoreAggregation>chunk(CHUNK_SIZE, transactionManager)
                .reader(monthlyReader(startDate, requestDate))
                .writer(new MonthlyRankWriter(monthlyProductRankRepository, requestDate))
                .listener(stepMonitorListener)
                .build();
    }

    private JdbcCursorItemReader<ProductScoreAggregation> weeklyReader(
            LocalDate startDate, LocalDate endDate
    ) {
        return RankAggregationReader.create("weeklyRankReader", dataSource, startDate, endDate);
    }

    private JdbcCursorItemReader<ProductScoreAggregation> monthlyReader(
            LocalDate startDate, LocalDate endDate
    ) {
        return RankAggregationReader.create("monthlyRankReader", dataSource, startDate, endDate);
    }

    private void validateRequestDate(LocalDate requestDate) {
        if (requestDate == null) {
            throw new RuntimeException("requestDate is required");
        }
        if (requestDate.isAfter(LocalDate.now())) {
            throw new RuntimeException("requestDate는 미래 날짜일 수 없습니다: " + requestDate);
        }
    }
}
