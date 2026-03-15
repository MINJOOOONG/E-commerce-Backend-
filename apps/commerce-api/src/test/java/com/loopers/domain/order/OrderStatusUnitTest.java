package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusUnitTest {

    private Order createOrder() {
        OrderItem item = new OrderItem(1L, "상품A", 10000L, 1);
        return new Order(1L, List.of(item));
    }

    @DisplayName("주문 생성 시,")
    @Nested
    class Create {

        @DisplayName("초기 상태는 PENDING_PAYMENT이다.")
        @Test
        void initialStatus_isPendingPayment() {
            // arrange & act
            Order order = createOrder();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        }
    }

    @DisplayName("markPaid()를 호출할 때,")
    @Nested
    class MarkPaid {

        @DisplayName("PENDING_PAYMENT 상태이면, PAID로 전이된다.")
        @Test
        void transitionsToPaid_whenPendingPayment() {
            // arrange
            Order order = createOrder();

            // act
            order.markPaid();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @DisplayName("이미 PAID 상태이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflict_whenAlreadyPaid() {
            // arrange
            Order order = createOrder();
            order.markPaid();

            // act
            CoreException result = assertThrows(CoreException.class, order::markPaid);

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }

        @DisplayName("CANCELLED 상태이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflict_whenCancelled() {
            // arrange
            Order order = createOrder();
            order.cancel();

            // act
            CoreException result = assertThrows(CoreException.class, order::markPaid);

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("cancel()을 호출할 때,")
    @Nested
    class Cancel {

        @DisplayName("PENDING_PAYMENT 상태이면, CANCELLED로 전이된다.")
        @Test
        void transitionsToCancelled_whenPendingPayment() {
            // arrange
            Order order = createOrder();

            // act
            order.cancel();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @DisplayName("PAID 상태이면, CANCELLED로 전이된다.")
        @Test
        void transitionsToCancelled_whenPaid() {
            // arrange
            Order order = createOrder();
            order.markPaid();

            // act
            order.cancel();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @DisplayName("이미 CANCELLED 상태이면, 상태가 유지된다. (멱등)")
        @Test
        void remainsCancelled_whenAlreadyCancelled() {
            // arrange
            Order order = createOrder();
            order.cancel();

            // act
            order.cancel();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }
}
