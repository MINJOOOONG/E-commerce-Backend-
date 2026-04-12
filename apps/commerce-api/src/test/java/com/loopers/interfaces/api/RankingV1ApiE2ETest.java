package com.loopers.interfaces.api;

import com.loopers.interfaces.api.ranking.RankingV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RankingV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/rankings";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS product_metrics (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    product_id BIGINT NOT NULL,
                    metric_date DATE NOT NULL,
                    score DOUBLE NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    deleted_at DATETIME(6),
                    UNIQUE KEY uk_product_metrics (product_id, metric_date),
                    INDEX idx_metric_date (metric_date)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mv_product_rank_weekly (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    product_id BIGINT NOT NULL,
                    total_score DOUBLE NOT NULL,
                    ranking INT NOT NULL,
                    base_date DATE NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    deleted_at DATETIME(6),
                    UNIQUE KEY uk_weekly_rank (base_date, product_id),
                    INDEX idx_weekly_base_date (base_date)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mv_product_rank_monthly (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    product_id BIGINT NOT NULL,
                    total_score DOUBLE NOT NULL,
                    ranking INT NOT NULL,
                    base_date DATE NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    deleted_at DATETIME(6),
                    UNIQUE KEY uk_monthly_rank (base_date, product_id),
                    INDEX idx_monthly_base_date (base_date)
                )
                """);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE product_metrics");
        jdbcTemplate.execute("TRUNCATE TABLE mv_product_rank_weekly");
        jdbcTemplate.execute("TRUNCATE TABLE mv_product_rank_monthly");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/rankings - DAILY 랭킹 조회")
    @Nested
    class DailyRanking {

        @DisplayName("해당 날짜의 product_metrics를 점수 내림차순으로 조회한다.")
        @Test
        void returnsDailyRanking() {
            // arrange
            LocalDate date = LocalDate.of(2026, 4, 12);
            insertProductMetrics(1L, date, 300.0);
            insertProductMetrics(2L, date, 100.0);
            insertProductMetrics(3L, date, 200.0);

            String url = ENDPOINT + "?period=DAILY&date=2026-04-12";

            // act
            var response = exchange(url);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().rankings()).hasSize(3),
                () -> assertThat(response.getBody().data().rankings().get(0).ranking()).isEqualTo(1),
                () -> assertThat(response.getBody().data().rankings().get(0).productId()).isEqualTo(1L),
                () -> assertThat(response.getBody().data().rankings().get(0).score()).isEqualTo(300.0),
                () -> assertThat(response.getBody().data().rankings().get(1).ranking()).isEqualTo(2),
                () -> assertThat(response.getBody().data().rankings().get(1).productId()).isEqualTo(3L),
                () -> assertThat(response.getBody().data().rankings().get(2).ranking()).isEqualTo(3),
                () -> assertThat(response.getBody().data().rankings().get(2).productId()).isEqualTo(2L)
            );
        }

        @DisplayName("size 파라미터로 조회 개수를 제한할 수 있다.")
        @Test
        void limitsBySize() {
            // arrange
            LocalDate date = LocalDate.of(2026, 4, 12);
            for (long i = 1; i <= 5; i++) {
                insertProductMetrics(i, date, (double) i * 10);
            }

            String url = ENDPOINT + "?period=DAILY&date=2026-04-12&size=3";

            // act
            var response = exchange(url);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().rankings()).hasSize(3),
                () -> assertThat(response.getBody().data().rankings().get(0).productId()).isEqualTo(5L)
            );
        }
    }

    @DisplayName("GET /api/v1/rankings - WEEKLY 랭킹 조회")
    @Nested
    class WeeklyRanking {

        @DisplayName("해당 날짜의 주간 랭킹을 순위 오름차순으로 조회한다.")
        @Test
        void returnsWeeklyRanking() {
            // arrange
            LocalDate baseDate = LocalDate.of(2026, 4, 12);
            insertWeeklyRank(1L, 700.0, 1, baseDate);
            insertWeeklyRank(3L, 600.0, 2, baseDate);
            insertWeeklyRank(2L, 350.0, 3, baseDate);

            String url = ENDPOINT + "?period=WEEKLY&date=2026-04-12";

            // act
            var response = exchange(url);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().rankings()).hasSize(3),
                () -> assertThat(response.getBody().data().rankings().get(0).ranking()).isEqualTo(1),
                () -> assertThat(response.getBody().data().rankings().get(0).productId()).isEqualTo(1L),
                () -> assertThat(response.getBody().data().rankings().get(0).score()).isEqualTo(700.0),
                () -> assertThat(response.getBody().data().rankings().get(1).ranking()).isEqualTo(2),
                () -> assertThat(response.getBody().data().rankings().get(2).ranking()).isEqualTo(3)
            );
        }
    }

    @DisplayName("GET /api/v1/rankings - MONTHLY 랭킹 조회")
    @Nested
    class MonthlyRanking {

        @DisplayName("해당 날짜의 월간 랭킹을 순위 오름차순으로 조회한다.")
        @Test
        void returnsMonthlyRanking() {
            // arrange
            LocalDate baseDate = LocalDate.of(2026, 4, 12);
            insertMonthlyRank(2L, 450.0, 1, baseDate);
            insertMonthlyRank(1L, 300.0, 2, baseDate);

            String url = ENDPOINT + "?period=MONTHLY&date=2026-04-12";

            // act
            var response = exchange(url);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().rankings()).hasSize(2),
                () -> assertThat(response.getBody().data().rankings().get(0).ranking()).isEqualTo(1),
                () -> assertThat(response.getBody().data().rankings().get(0).productId()).isEqualTo(2L),
                () -> assertThat(response.getBody().data().rankings().get(0).score()).isEqualTo(450.0),
                () -> assertThat(response.getBody().data().rankings().get(1).ranking()).isEqualTo(2),
                () -> assertThat(response.getBody().data().rankings().get(1).productId()).isEqualTo(1L)
            );
        }
    }

    @DisplayName("엣지 케이스")
    @Nested
    class EdgeCase {

        @DisplayName("데이터가 없으면 빈 리스트를 반환한다.")
        @Test
        void returnsEmptyListWhenNoData() {
            // arrange
            String url = ENDPOINT + "?period=DAILY&date=2026-04-12";

            // act
            var response = exchange(url);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().rankings()).isEmpty()
            );
        }

        @DisplayName("잘못된 period 값이면 400을 반환한다.")
        @Test
        void returnsBadRequestForInvalidPeriod() {
            // arrange
            String url = ENDPOINT + "?period=INVALID&date=2026-04-12";

            // act
            var response = testRestTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(null),
                    new ParameterizedTypeReference<ApiResponse<Object>>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("period 파라미터가 없으면 400을 반환한다.")
        @Test
        void returnsBadRequestWithoutPeriod() {
            // arrange
            String url = ENDPOINT + "?date=2026-04-12";

            // act
            var response = testRestTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(null),
                    new ParameterizedTypeReference<ApiResponse<Object>>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("date 파라미터가 없으면 400을 반환한다.")
        @Test
        void returnsBadRequestWithoutDate() {
            // arrange
            String url = ENDPOINT + "?period=DAILY";

            // act
            var response = testRestTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(null),
                    new ParameterizedTypeReference<ApiResponse<Object>>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // --- Helper Methods ---

    private ResponseEntity<ApiResponse<RankingV1Dto.RankingListResponse>> exchange(String url) {
        return testRestTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
        );
    }

    private void insertProductMetrics(Long productId, LocalDate metricDate, double score) {
        jdbcTemplate.update(
                "INSERT INTO product_metrics (product_id, metric_date, score, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                productId, metricDate, score
        );
    }

    private void insertWeeklyRank(Long productId, double totalScore, int ranking, LocalDate baseDate) {
        jdbcTemplate.update(
                "INSERT INTO mv_product_rank_weekly (product_id, total_score, ranking, base_date, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
                productId, totalScore, ranking, baseDate
        );
    }

    private void insertMonthlyRank(Long productId, double totalScore, int ranking, LocalDate baseDate) {
        jdbcTemplate.update(
                "INSERT INTO mv_product_rank_monthly (product_id, total_score, ranking, base_date, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
                productId, totalScore, ranking, baseDate
        );
    }
}
