package com.loopers.domain.productmetrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductMetricsUnitTest {

    @DisplayName("ProductMetrics 생성")
    @Nested
    class Create {

        @DisplayName("유효한 값으로 생성하면 성공한다.")
        @Test
        void createsWithValidValues() {
            // arrange & act
            var metrics = new ProductMetrics(1L, LocalDate.of(2026, 4, 12), 150.5);

            // assert
            assertThat(metrics.getProductId()).isEqualTo(1L);
            assertThat(metrics.getMetricDate()).isEqualTo(LocalDate.of(2026, 4, 12));
            assertThat(metrics.getScore()).isEqualTo(150.5);
        }

        @DisplayName("productId가 null이면 생성에 실패한다.")
        @Test
        void failsWithNullProductId() {
            assertThatThrownBy(() -> new ProductMetrics(null, LocalDate.of(2026, 4, 12), 100.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("metricDate가 null이면 생성에 실패한다.")
        @Test
        void failsWithNullMetricDate() {
            assertThatThrownBy(() -> new ProductMetrics(1L, null, 100.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("score가 음수이면 생성에 실패한다.")
        @Test
        void failsWithNegativeScore() {
            assertThatThrownBy(() -> new ProductMetrics(1L, LocalDate.of(2026, 4, 12), -1.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
