package com.loopers.domain.productrank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class MonthlyProductRankUnitTest {

    @DisplayName("MonthlyProductRank 생성")
    @Nested
    class Create {

        @DisplayName("유효한 값으로 생성하면 성공한다.")
        @Test
        void createsWithValidValues() {
            // arrange & act
            var rank = new MonthlyProductRank(1L, 1200.0, 1, LocalDate.of(2026, 4, 12));

            // assert
            assertAll(
                () -> assertThat(rank.getProductId()).isEqualTo(1L),
                () -> assertThat(rank.getTotalScore()).isEqualTo(1200.0),
                () -> assertThat(rank.getRanking()).isEqualTo(1),
                () -> assertThat(rank.getBaseDate()).isEqualTo(LocalDate.of(2026, 4, 12))
            );
        }

        @DisplayName("productId가 null이면 생성에 실패한다.")
        @Test
        void failsWithNullProductId() {
            assertThatThrownBy(() -> new MonthlyProductRank(null, 100.0, 1, LocalDate.of(2026, 4, 12)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("totalScore가 음수이면 생성에 실패한다.")
        @Test
        void failsWithNegativeTotalScore() {
            assertThatThrownBy(() -> new MonthlyProductRank(1L, -1.0, 1, LocalDate.of(2026, 4, 12)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("ranking이 0 이하이면 생성에 실패한다.")
        @Test
        void failsWithZeroRanking() {
            assertThatThrownBy(() -> new MonthlyProductRank(1L, 100.0, 0, LocalDate.of(2026, 4, 12)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("baseDate가 null이면 생성에 실패한다.")
        @Test
        void failsWithNullBaseDate() {
            assertThatThrownBy(() -> new MonthlyProductRank(1L, 100.0, 1, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
