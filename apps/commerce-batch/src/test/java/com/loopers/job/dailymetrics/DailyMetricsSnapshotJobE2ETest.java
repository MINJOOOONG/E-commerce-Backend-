package com.loopers.job.dailymetrics;

import com.loopers.batch.job.dailymetrics.DailyMetricsSnapshotJobConfig;
import com.loopers.domain.productmetrics.ProductMetrics;
import com.loopers.infrastructure.productmetrics.ProductMetricsJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = "spring.batch.job.name=" + DailyMetricsSnapshotJobConfig.JOB_NAME)
class DailyMetricsSnapshotJobE2ETest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier(DailyMetricsSnapshotJobConfig.JOB_NAME)
    private Job job;

    @Autowired
    private ProductMetricsJpaRepository productMetricsJpaRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    private static final String KEY_PREFIX = "ranking:all:";

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    @DisplayName("일간 스냅샷 배치")
    @Nested
    class DailySnapshot {

        @DisplayName("requestDate 파라미터가 없으면 배치가 실패한다.")
        @Test
        void failsWithoutRequestDate() throws Exception {
            // arrange
            jobLauncherTestUtils.setJob(job);

            // act
            var jobExecution = jobLauncherTestUtils.launchJob();

            // assert
            assertAll(
                () -> assertThat(jobExecution).isNotNull(),
                () -> assertThat(jobExecution.getExitStatus().getExitCode())
                        .isEqualTo(ExitStatus.FAILED.getExitCode())
            );
        }

        @DisplayName("Redis에 일간 점수가 있으면 product_metrics에 정확히 적재된다.")
        @Test
        void snapshotsRedisScoresToProductMetrics() throws Exception {
            // arrange
            jobLauncherTestUtils.setJob(job);

            String dateKey = "20260412";
            String redisKey = KEY_PREFIX + dateKey;
            redisTemplate.opsForZSet().add(redisKey, "1", 150.5);
            redisTemplate.opsForZSet().add(redisKey, "2", 300.0);
            redisTemplate.opsForZSet().add(redisKey, "3", 75.2);

            var jobParameters = new JobParametersBuilder()
                    .addLocalDate("requestDate", LocalDate.of(2026, 4, 12))
                    .addLong("run.id", 100L)
                    .toJobParameters();

            // act
            var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // assert
            assertThat(jobExecution.getExitStatus().getExitCode())
                    .isEqualTo(ExitStatus.COMPLETED.getExitCode());

            List<ProductMetrics> metrics = productMetricsJpaRepository.findAll();
            assertAll(
                () -> assertThat(metrics).hasSize(3),
                () -> assertThat(metrics)
                        .extracting(ProductMetrics::getProductId)
                        .containsExactlyInAnyOrder(1L, 2L, 3L),
                () -> assertThat(metrics)
                        .filteredOn(m -> m.getProductId().equals(2L))
                        .first()
                        .satisfies(m -> assertThat(m.getScore()).isEqualTo(300.0))
            );
        }

        @DisplayName("같은 날짜로 재실행하면 기존 데이터를 교체한다(멱등성).")
        @Test
        void replacesExistingDataOnRerun() throws Exception {
            // arrange
            jobLauncherTestUtils.setJob(job);

            String dateKey = "20260412";
            String redisKey = KEY_PREFIX + dateKey;
            LocalDate metricDate = LocalDate.of(2026, 4, 12);

            // 1차 실행 — 점수 100
            redisTemplate.opsForZSet().add(redisKey, "1", 100.0);

            var jobParameters1 = new JobParametersBuilder()
                    .addLocalDate("requestDate", metricDate)
                    .addLong("run.id", 1L)
                    .toJobParameters();
            jobLauncherTestUtils.launchJob(jobParameters1);

            // 점수 변경 — 200으로
            redisTemplate.opsForZSet().add(redisKey, "1", 200.0);

            // 2차 실행
            var jobParameters2 = new JobParametersBuilder()
                    .addLocalDate("requestDate", metricDate)
                    .addLong("run.id", 2L)
                    .toJobParameters();

            // act
            var jobExecution = jobLauncherTestUtils.launchJob(jobParameters2);

            // assert
            assertThat(jobExecution.getExitStatus().getExitCode())
                    .isEqualTo(ExitStatus.COMPLETED.getExitCode());

            List<ProductMetrics> metrics = productMetricsJpaRepository.findByMetricDate(metricDate);
            assertAll(
                () -> assertThat(metrics).hasSize(1),
                () -> assertThat(metrics.get(0).getScore()).isEqualTo(200.0)
            );
        }

        @DisplayName("Redis에 해당 날짜 데이터가 없으면 빈 결과로 정상 완료된다.")
        @Test
        void completesWithEmptyRedisData() throws Exception {
            // arrange
            jobLauncherTestUtils.setJob(job);

            var jobParameters = new JobParametersBuilder()
                    .addLocalDate("requestDate", LocalDate.of(2026, 4, 12))
                    .addLong("run.id", 200L)
                    .toJobParameters();

            // act
            var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // assert
            assertAll(
                () -> assertThat(jobExecution.getExitStatus().getExitCode())
                        .isEqualTo(ExitStatus.COMPLETED.getExitCode()),
                () -> assertThat(productMetricsJpaRepository.findAll()).isEmpty()
            );
        }
    }
}
