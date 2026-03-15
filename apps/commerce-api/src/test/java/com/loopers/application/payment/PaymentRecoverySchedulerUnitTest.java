package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PaymentRecoverySchedulerUnitTest {

    private PaymentRecoveryScheduler scheduler;
    private PaymentFacade paymentFacade;
    private FakePgClient fakePgClient;
    private PaymentFacadeUnitTest.SimpleOrderRepository orderRepository;
    private PaymentFacadeUnitTest.SimpleProductRepository productRepository;
    private PaymentFacadeUnitTest.SimpleUserRepository userRepository;
    private PaymentFacadeUnitTest.SimpleUserCouponRepository userCouponRepository;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        orderRepository = new PaymentFacadeUnitTest.SimpleOrderRepository();
        productRepository = new PaymentFacadeUnitTest.SimpleProductRepository();
        userRepository = new PaymentFacadeUnitTest.SimpleUserRepository();
        userCouponRepository = new PaymentFacadeUnitTest.SimpleUserCouponRepository();
        FakePaymentRepository paymentRepository = new FakePaymentRepository();
        paymentService = new PaymentService(paymentRepository);
        fakePgClient = new FakePgClient();

        PaymentTransactionService txService = new PaymentTransactionService(
            paymentService, orderRepository, productRepository, userRepository, userCouponRepository
        );
        paymentFacade = new PaymentFacade(txService, paymentService, fakePgClient);
        scheduler = new PaymentRecoveryScheduler(paymentService, txService, fakePgClient);
    }

    private Order createOrderWithItems() {
        Product product = new Product(1L, "상품A", 10000L, "설명", 10);
        productRepository.addWithId(1L, product);

        User user = new User("테스트유저", 100000L);
        userRepository.addWithId(1L, user);

        OrderItem item = new OrderItem(1L, "상품A", 10000L, 2);
        Order order = new Order(1L, List.of(item));

        product.decreaseStock(2);
        user.deductPoint(order.getFinalPrice());

        OrderService orderService = new OrderService(orderRepository);
        orderService.createOrder(order);

        return order;
    }

    @DisplayName("Scheduler 복구를 실행할 때,")
    @Nested
    class Recover {

        @DisplayName("PG 조회 결과가 승인이면, Payment는 APPROVED, Order는 PAID가 된다.")
        @Test
        void approvesPayment_whenPgConfirmsSuccess() {
            // arrange
            Order order = createOrderWithItems();
            PaymentInfo requested = paymentFacade.requestPayment(order.getId(), PaymentMethod.CARD);
            fakePgClient.setQueryResult(true);

            // act
            scheduler.recover();

            // assert
            Payment payment = paymentService.getPaymentByPgTransactionId(requested.pgTransactionId());
            assertAll(
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID)
            );
        }

        @DisplayName("PG 조회 결과가 실패이면, Payment는 FAILED, Order는 CANCELLED이고 보상 처리된다.")
        @Test
        void failsPayment_whenPgConfirmsFailure() {
            // arrange
            Order order = createOrderWithItems();
            paymentFacade.requestPayment(order.getId(), PaymentMethod.CARD);
            fakePgClient.setQueryResult(false);

            // act
            scheduler.recover();

            // assert
            assertAll(
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED),
                () -> assertThat(userRepository.findById(1L).get().getPoint())
                    .as("포인트 환불").isEqualTo(100000L),
                () -> assertThat(productRepository.findById(1L).get().getStockQuantity().value())
                    .as("재고 복구").isEqualTo(10)
            );
        }

        @DisplayName("이미 callback으로 처리된 Payment는 skip한다.")
        @Test
        void skipsPayment_whenAlreadyProcessedByCallback() {
            // arrange
            Order order = createOrderWithItems();
            PaymentInfo requested = paymentFacade.requestPayment(order.getId(), PaymentMethod.CARD);
            paymentFacade.handlePgCallback(requested.pgTransactionId(), true, "APPROVED");

            // act — scheduler가 이미 APPROVED인 payment를 다시 처리해도 에러 없음
            scheduler.recover();

            // assert
            Payment payment = paymentService.getPaymentByPgTransactionId(requested.pgTransactionId());
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        }

        @DisplayName("pgTransactionId가 없는 Payment는 skip한다.")
        @Test
        void skipsPayment_whenPgTransactionIdIsNull() {
            // arrange — PG 호출 중 예외가 발생하여 pgTransactionId가 없는 상태
            Order order = createOrderWithItems();
            fakePgClient.setShouldThrow(true);
            paymentFacade.requestPayment(order.getId(), PaymentMethod.CARD);
            fakePgClient.setShouldThrow(false);

            // act
            scheduler.recover();

            // assert — PENDING 상태 그대로 유지
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        }
    }
}
