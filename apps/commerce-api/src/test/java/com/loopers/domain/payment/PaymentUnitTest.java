package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentUnitTest {

    private Payment createPayment() {
        return new Payment(1L, 1L, 10000L, PaymentMethod.CARD);
    }

    @DisplayName("결제를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("초기 상태는 PENDING이다.")
        @Test
        void initialStatus_isPending() {
            // arrange & act
            Payment payment = createPayment();

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @DisplayName("필드값이 올바르게 저장된다.")
        @Test
        void fieldsAreStoredCorrectly() {
            // arrange & act
            Payment payment = new Payment(10L, 20L, 50000L, PaymentMethod.CARD);

            // assert
            assertAll(
                () -> assertThat(payment.getOrderId()).isEqualTo(10L),
                () -> assertThat(payment.getUserId()).isEqualTo(20L),
                () -> assertThat(payment.getAmount()).isEqualTo(50000L),
                () -> assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CARD),
                () -> assertThat(payment.getPgTransactionId()).isNull(),
                () -> assertThat(payment.getPgResponseCode()).isNull(),
                () -> assertThat(payment.getApprovedAt()).isNull()
            );
        }

        @DisplayName("orderId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenOrderIdIsNull() {
            // act
            CoreException result = assertThrows(CoreException.class, () ->
                new Payment(null, 1L, 10000L, PaymentMethod.CARD));

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("금액이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenAmountIsZeroOrNegative() {
            // act
            CoreException result = assertThrows(CoreException.class, () ->
                new Payment(1L, 1L, 0L, PaymentMethod.CARD));

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("approve()를 호출할 때,")
    @Nested
    class Approve {

        @DisplayName("PENDING 상태이면, APPROVED로 전이되고 PG 정보가 기록된다.")
        @Test
        void transitionsToApproved_whenPending() {
            // arrange
            Payment payment = createPayment();

            // act
            payment.approve("pg-tx-001", "SUCCESS");

            // assert
            assertAll(
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED),
                () -> assertThat(payment.getPgTransactionId()).isEqualTo("pg-tx-001"),
                () -> assertThat(payment.getPgResponseCode()).isEqualTo("SUCCESS"),
                () -> assertThat(payment.getApprovedAt()).isNotNull()
            );
        }

        @DisplayName("이미 APPROVED 상태이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflict_whenAlreadyApproved() {
            // arrange
            Payment payment = createPayment();
            payment.approve("pg-tx-001", "SUCCESS");

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                payment.approve("pg-tx-002", "SUCCESS"));

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }

        @DisplayName("FAILED 상태이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflict_whenFailed() {
            // arrange
            Payment payment = createPayment();
            payment.fail("CARD_DECLINED");

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                payment.approve("pg-tx-001", "SUCCESS"));

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }

        @DisplayName("CANCELLED 상태이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflict_whenCancelled() {
            // arrange
            Payment payment = createPayment();
            payment.approve("pg-tx-001", "SUCCESS");
            payment.cancel();

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                payment.approve("pg-tx-002", "SUCCESS"));

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("fail()을 호출할 때,")
    @Nested
    class Fail {

        @DisplayName("PENDING 상태이면, FAILED로 전이되고 응답 코드가 기록된다.")
        @Test
        void transitionsToFailed_whenPending() {
            // arrange
            Payment payment = createPayment();

            // act
            payment.fail("CARD_DECLINED");

            // assert
            assertAll(
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED),
                () -> assertThat(payment.getPgResponseCode()).isEqualTo("CARD_DECLINED")
            );
        }

        @DisplayName("APPROVED 상태이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflict_whenApproved() {
            // arrange
            Payment payment = createPayment();
            payment.approve("pg-tx-001", "SUCCESS");

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                payment.fail("ERROR"));

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }

        @DisplayName("이미 FAILED 상태이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflict_whenAlreadyFailed() {
            // arrange
            Payment payment = createPayment();
            payment.fail("CARD_DECLINED");

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                payment.fail("ANOTHER_ERROR"));

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("cancel()을 호출할 때,")
    @Nested
    class Cancel {

        @DisplayName("APPROVED 상태이면, CANCELLED로 전이된다.")
        @Test
        void transitionsToCancelled_whenApproved() {
            // arrange
            Payment payment = createPayment();
            payment.approve("pg-tx-001", "SUCCESS");

            // act
            payment.cancel();

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        @DisplayName("PENDING 상태이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflict_whenPending() {
            // arrange
            Payment payment = createPayment();

            // act
            CoreException result = assertThrows(CoreException.class, payment::cancel);

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }

        @DisplayName("이미 CANCELLED 상태이면, 상태가 유지된다. (멱등)")
        @Test
        void remainsCancelled_whenAlreadyCancelled() {
            // arrange
            Payment payment = createPayment();
            payment.approve("pg-tx-001", "SUCCESS");
            payment.cancel();

            // act
            payment.cancel();

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }
    }
}
