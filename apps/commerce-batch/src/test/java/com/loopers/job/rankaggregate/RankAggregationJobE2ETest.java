package com.loopers.job.rankaggregate;

import com.loopers.batch.job.rankaggregate.RankAggregationJobConfig;
import com.loopers.domain.productmetrics.ProductMetrics;
import com.loopers.domain.productrank.MonthlyProductRank;
import com.loopers.domain.productrank.WeeklyProductRank;
import com.loopers.infrastructure.productmetrics.ProductMetricsJpaRepository;
import com.loopers.infrastructure.productrank.MonthlyProductRankJpaRepository;
import com.loopers.infrastructure.productrank.WeeklyProductRankJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
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
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = "spring.batch.job.name=" + RankAggregationJobConfig.JOB_NAME)
class RankAggregationJobE2ETest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier(RankAggregationJobConfig.JOB_NAME)
    private Job job;

    @Autowired
    private ProductMetricsJpaRepository productMetricsJpaRepository;

    @Autowired
    private WeeklyProductRankJpaRepository weeklyProductRankJpaRepository;

    @Autowired
    private MonthlyProductRankJpaRepository monthlyProductRankJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 4, 12);

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주간 랭킹 집계")
    @Nested
    class WeeklyRank {

        @DisplayName("최근 7일 product_metrics를 상품별로 합산하여 weekly 랭킹을 적재한다.")
        @Test
        void aggregatesWeeklyRanking() throws Exception {
            // arrange
            jobLauncherTestUtils.setJob(job);

            // 상품1: 7일간 매일 100점 = 합계 700
            // 상품2: 7일간 매일 50점 = 합계 350
            // 상품3: 3일만 200점 = 합계 600
            for (int i = 0; i < 7; i++) {
                LocalDate date = BASE_DATE.minusDays(i);
                productMetricsJpaRepository.save(new ProductMetrics(1L, date, 100.0));
                productMetricsJpaRepository.save(new ProductMetrics(2L, date, 50.0));
            }
            for (int i = 0; i < 3; i++) {
                LocalDate date = BASE_DATE.minusDays(i);
                productMetricsJpaRepository.save(new ProductMetrics(3L, date, 200.0));
            }

            var jobParameters = new JobParametersBuilder()
                    .addLocalDate("requestDate", BASE_DATE)
                    .addLong("run.id", 100L)
                    .toJobParameters();

            // act
            var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // assert
            assertThat(jobExecution.getExitStatus().getExitCode())
                    .isEqualTo(ExitStatus.COMPLETED.getExitCode());

            List<WeeklyProductRank> weeklyRanks = weeklyProductRankJpaRepository.findAll();
            assertAll(
                () -> assertThat(weeklyRanks).hasSize(3),
                // 1위: 상품1 (700점)
                () -> assertThat(weeklyRanks)
                        .filteredOn(r -> r.getRanking() == 1)
                        .first()
                        .satisfies(r -> {
                            assertThat(r.getProductId()).isEqualTo(1L);
                            assertThat(r.getTotalScore()).isEqualTo(700.0);
                        }),
                // 2위: 상품3 (600점)
                () -> assertThat(weeklyRanks)
                        .filteredOn(r -> r.getRanking() == 2)
                        .first()
                        .satisfies(r -> {
                            assertThat(r.getProductId()).isEqualTo(3L);
                            assertThat(r.getTotalScore()).isEqualTo(600.0);
                        }),
                // 3위: 상품2 (350점)
                () -> assertThat(weeklyRanks)
                        .filteredOn(r -> r.getRanking() == 3)
                        .first()
                        .satisfies(r -> {
                            assertThat(r.getProductId()).isEqualTo(2L);
                            assertThat(r.getTotalScore()).isEqualTo(350.0);
                        })
            );
        }

        @DisplayName("7일 범위 밖의 데이터는 주간 집계에 포함되지 않는다.")
        @Test
        void excludesDataOutsideWeeklyRange() throws Exception {
            // arrange
            jobLauncherTestUtils.setJob(job);

            // 범위 내 (requestDate - 6 ~ requestDate)
            productMetricsJpaRepository.save(new ProductMetrics(1L, BASE_DATE, 100.0));
            // 범위 밖 (7일 전 = requestDate - 7)
            productMetricsJpaRepository.save(new ProductMetrics(2L, BASE_DATE.minusDays(7), 500.0));

            var jobParameters = new JobParametersBuilder()
                    .addLocalDate("requestDate", BASE_DATE)
                    .addLong("run.id", 200L)
                    .toJobParameters();

            // act
            var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // assert
            assertThat(jobExecution.getExitStatus().getExitCode())
                    .isEqualTo(ExitStatus.COMPLETED.getExitCode());

            List<WeeklyProductRank> weeklyRanks = weeklyProductRankJpaRepository.findAll();
            assertAll(
                () -> assertThat(weeklyRanks).hasSize(1),
                () -> assertThat(weeklyRanks.get(0).getProductId()).isEqualTo(1L),
                () -> assertThat(weeklyRanks.get(0).getTotalScore()).isEqualTo(100.0)
            );
        }
    }

    @DisplayName("월간 랭킹 집계")
    @Nested
    class MonthlyRank {

        @DisplayName("최근 30일 product_metrics를 상품별로 합산하여 monthly 랭킹을 적재한다.")
        @Test
        void aggregatesMonthlyRanking() throws Exception {
            // arrange
            jobLauncherTestUtils.setJob(job);

            // 상품1: 30일간 매일 10점 = 합계 300
            // 상품2: 15일만 30점 = 합계 450
            for (int i = 0; i < 30; i++) {
                LocalDate date = BASE_DATE.minusDays(i);
                productMetricsJpaRepository.save(new ProductMetrics(1L, date, 10.0));
            }
            for (int i = 0; i < 15; i++) {
                LocalDate date = BASE_DATE.minusDays(i);
                productMetricsJpaRepository.save(new ProductMetrics(2L, date, 30.0));
            }

            var jobParameters = new JobParametersBuilder()
                    .addLocalDate("requestDate", BASE_DATE)
                    .addLong("run.id", 300L)
                    .toJobParameters();

            // act
            var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // assert
            assertThat(jobExecution.getExitStatus().getExitCode())
                    .isEqualTo(ExitStatus.COMPLETED.getExitCode());

            List<MonthlyProductRank> monthlyRanks = monthlyProductRankJpaRepository.findAll();
            assertAll(
                () -> assertThat(monthlyRanks).hasSize(2),
                // 1위: 상품2 (450점)
                () -> assertThat(monthlyRanks)
                        .filteredOn(r -> r.getRanking() == 1)
                        .first()
                        .satisfies(r -> {
                            assertThat(r.getProductId()).isEqualTo(2L);
                            assertThat(r.getTotalScore()).isEqualTo(450.0);
                        }),
                // 2위: 상품1 (300점)
                () -> assertThat(monthlyRanks)
                        .filteredOn(r -> r.getRanking() == 2)
                        .first()
                        .satisfies(r -> {
                            assertThat(r.getProductId()).isEqualTo(1L);
                            assertThat(r.getTotalScore()).isEqualTo(300.0);
                        })
            );
        }
    }

    @DisplayName("TOP 100 절단")
    @Nested
    class Top100Limit {

        @DisplayName("상품이 100개를 초과하면 TOP 100만 적재된다.")
        @Test
        void limitsToTop100() throws Exception {
            // arrange
            jobLauncherTestUtils.setJob(job);

            // 120개 상품 데이터 생성 (productId: 1~120)
            for (long productId = 1; productId <= 120; productId++) {
                productMetricsJpaRepository.save(
                        new ProductMetrics(productId, BASE_DATE, (double) productId)
                );
            }

            var jobParameters = new JobParametersBuilder()
                    .addLocalDate("requestDate", BASE_DATE)
                    .addLong("run.id", 400L)
                    .toJobParameters();

            // act
            var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // assert
            assertThat(jobExecution.getExitStatus().getExitCode())
                    .isEqualTo(ExitStatus.COMPLETED.getExitCode());

            List<WeeklyProductRank> weeklyRanks = weeklyProductRankJpaRepository.findAll();
            List<MonthlyProductRank> monthlyRanks = monthlyProductRankJpaRepository.findAll();

            assertAll(
                () -> assertThat(weeklyRanks).hasSize(100),
                () -> assertThat(monthlyRanks).hasSize(100),
                // 1위는 점수가 가장 높은 productId=120
                () -> assertThat(weeklyRanks)
                        .filteredOn(r -> r.getRanking() == 1)
                        .first()
                        .satisfies(r -> assertThat(r.getProductId()).isEqualTo(120L)),
                // 100위는 productId=21 (120 - 99)
                () -> assertThat(weeklyRanks)
                        .filteredOn(r -> r.getRanking() == 100)
                        .first()
                        .satisfies(r -> assertThat(r.getProductId()).isEqualTo(21L))
            );
        }
    }

    @DisplayName("멱등성")
    @Nested
    class Idempotency {

        @DisplayName("같은 requestDate로 재실행하면 기존 데이터를 교체한다.")
        @Test
        void replacesExistingDataOnRerun() throws Exception {
            // arrange
            jobLauncherTestUtils.setJob(job);

            // 1차 실행 — 상품1 점수 100
            productMetricsJpaRepository.save(new ProductMetrics(1L, BASE_DATE, 100.0));

            var jobParameters1 = new JobParametersBuilder()
                    .addLocalDate("requestDate", BASE_DATE)
                    .addLong("run.id", 500L)
                    .toJobParameters();
            jobLauncherTestUtils.launchJob(jobParameters1);

            // 데이터 변경 — 상품2 추가
            productMetricsJpaRepository.save(new ProductMetrics(2L, BASE_DATE, 200.0));

            // 2차 실행
            var jobParameters2 = new JobParametersBuilder()
                    .addLocalDate("requestDate", BASE_DATE)
                    .addLong("run.id", 501L)
                    .toJobParameters();

            // act
            var jobExecution = jobLauncherTestUtils.launchJob(jobParameters2);

            // assert
            assertThat(jobExecution.getExitStatus().getExitCode())
                    .isEqualTo(ExitStatus.COMPLETED.getExitCode());

            List<WeeklyProductRank> weeklyRanks = weeklyProductRankJpaRepository.findAll();
            assertAll(
                () -> assertThat(weeklyRanks).hasSize(2),
                () -> assertThat(weeklyRanks)
                        .filteredOn(r -> r.getRanking() == 1)
                        .first()
                        .satisfies(r -> {
                            assertThat(r.getProductId()).isEqualTo(2L);
                            assertThat(r.getTotalScore()).isEqualTo(200.0);
                        })
            );
        }
    }

    @DisplayName("엣지 케이스")
    @Nested
    class EdgeCase {

        @DisplayName("product_metrics가 비어있으면 빈 결과로 정상 완료된다.")
        @Test
        void completesWithEmptyMetrics() throws Exception {
            // arrange
            jobLauncherTestUtils.setJob(job);

            var jobParameters = new JobParametersBuilder()
                    .addLocalDate("requestDate", BASE_DATE)
                    .addLong("run.id", 600L)
                    .toJobParameters();

            // act
            var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // assert
            assertAll(
                () -> assertThat(jobExecution.getExitStatus().getExitCode())
                        .isEqualTo(ExitStatus.COMPLETED.getExitCode()),
                () -> assertThat(weeklyProductRankJpaRepository.findAll()).isEmpty(),
                () -> assertThat(monthlyProductRankJpaRepository.findAll()).isEmpty()
            );
        }

        @DisplayName("requestDate가 없으면 배치가 실패한다.")
        @Test
        void failsWithoutRequestDate() throws Exception {
            // arrange
            jobLauncherTestUtils.setJob(job);

            // act
            var jobExecution = jobLauncherTestUtils.launchJob();

            // assert
            assertThat(jobExecution.getExitStatus().getExitCode())
                    .isEqualTo(ExitStatus.FAILED.getExitCode());
        }

        @DisplayName("미래 날짜의 requestDate가 주어지면 배치가 실패한다.")
        @Test
        void failsWithFutureRequestDate() throws Exception {
            // arrange
            jobLauncherTestUtils.setJob(job);

            LocalDate futureDate = LocalDate.now().plusDays(1);
            var jobParameters = new JobParametersBuilder()
                    .addLocalDate("requestDate", futureDate)
                    .addLong("run.id", 700L)
                    .toJobParameters();

            // act
            var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // assert
            assertThat(jobExecution.getExitStatus().getExitCode())
                    .isEqualTo(ExitStatus.FAILED.getExitCode());
        }
    }
}
