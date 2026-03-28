package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserUnitTest {

    @DisplayName("포인트를 환불할 때,")
    @Nested
    class RefundPoint {

        @DisplayName("정상 금액이면, 포인트가 증가한다.")
        @Test
        void increasesPoint_whenValidAmount() {
            // arrange
            User user = new User("테스트유저", 10000L);

            // act
            user.refundPoint(5000L);

            // assert
            assertThat(user.getPoint()).isEqualTo(15000L);
        }

        @DisplayName("금액이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenAmountIsZeroOrNegative() {
            // arrange
            User user = new User("테스트유저", 10000L);

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                user.refundPoint(0L));

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
