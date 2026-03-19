package com.loopers.application.payment;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.payment.PgClient;
import com.loopers.domain.payment.PgPaymentRequest;
import com.loopers.domain.payment.PgPaymentResponse;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class PaymentConcurrencyTest {

    @TestConfiguration
    static class TestPgClientConfig {
        @Bean
        @Primary
        PgClient testPgClient() {
            return new PgClient() {
                @Override
                public PgPaymentResponse requestPayment(PgPaymentRequest request) {
                    return new PgPaymentResponse(true, UUID.randomUUID().toString(), "APPROVED");
                }

                @Override
                public PgPaymentResponse queryPaymentStatus(String pgTransactionId) {
                    return new PgPaymentResponse(true, pgTransactionId, "APPROVED");
                }

                @Override
                public PgPaymentResponse cancelPayment(String pgTransactionId) {
                    return new PgPaymentResponse(true, pgTransactionId, "CANCELLED");
                }
            };
        }
    }

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private PaymentTransactionService transactionService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    /**
     * OrderFacade.createOrder()를 통해 정상 주문 흐름으로 생성한다.
     * 직접 repository.save()로 저장하면 OrderItem.orderId가 할당되지 않아
     * 'Column order_id cannot be null' 제약 위반이 발생한다.
     */
    private OrderInfo createOrderViaFacade() {
        User user = userRepository.save(new User("테스트유저", 100_000L));
        Product product = productRepository.save(
            new Product(1L, "테스트상품", 10_000L, "설명", 50)
        );

        return orderFacade.createOrder(
            user.getId(),
            List.of(new OrderFacade.OrderItemRequest(product.getId(), 2))
        );
    }

    @DisplayName("결제 승인 동시성 테스트: ")
    @Nested
    class ApproveConcurrency {

        @DisplayName("동일 Payment에 10개 스레드가 동시에 approve를 시도하면, 1건만 성공하고 상태는 APPROVED이다.")
        @Test
        void concurrentApprove_onlyOneSucceeds() throws InterruptedException {
            // arrange
            int threadCount = 10;
            OrderInfo orderInfo = createOrderViaFacade();
            PaymentInfo requested = paymentFacade.requestPayment(orderInfo.orderId(), PaymentMethod.CARD);

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // act
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        transactionService.approvePayment(
                            requested.paymentId(),
                            requested.pgTransactionId(),
                            "APPROVED"
                        );
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();
            doneLatch.await(10, TimeUnit.SECONDS);
            executorService.shutdown();

            // assert
            Payment payment = paymentRepository.findById(requested.paymentId()).orElseThrow();
            Order updatedOrder = orderRepository.findById(orderInfo.orderId()).orElseThrow();

            assertAll(
                () -> assertThat(successCount.get()).as("성공 건수").isEqualTo(1),
                () -> assertThat(failCount.get()).as("실패 건수").isEqualTo(threadCount - 1),
                () -> assertThat(payment.getStatus()).as("결제 상태").isEqualTo(PaymentStatus.APPROVED),
                () -> assertThat(updatedOrder.getStatus()).as("주문 상태").isEqualTo(OrderStatus.PAID)
            );
        }
    }

    @DisplayName("결제 요청 동시성 테스트: ")
    @Nested
    class RequestConcurrency {

        @DisplayName("동일 Order에 5개 스레드가 동시에 결제를 요청하면, 락에 의해 순차 처리되고 첫 결제 이후에는 CONFLICT가 발생한다.")
        @Test
        void concurrentPaymentRequest_serializedByLock() throws InterruptedException {
            // arrange
            int threadCount = 5;
            OrderInfo orderInfo = createOrderViaFacade();
            // 먼저 1건 결제 요청 → PENDING 상태 Payment 생성 → callback으로 PAID 전환
            PaymentInfo first = paymentFacade.requestPayment(orderInfo.orderId(), PaymentMethod.CARD);
            paymentFacade.handlePgCallback(first.pgTransactionId(), true, "APPROVED");

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // act — 이미 PAID 상태인 주문에 동시 결제 요청
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        paymentFacade.requestPayment(orderInfo.orderId(), PaymentMethod.CARD);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();
            doneLatch.await(10, TimeUnit.SECONDS);
            executorService.shutdown();

            // assert — PAID 상태의 주문이므로 전부 CONFLICT로 실패
            Order updatedOrder = orderRepository.findById(orderInfo.orderId()).orElseThrow();

            assertAll(
                () -> assertThat(successCount.get()).as("성공 건수").isEqualTo(0),
                () -> assertThat(failCount.get()).as("실패 건수").isEqualTo(threadCount),
                () -> assertThat(updatedOrder.getStatus()).as("주문 상태 유지").isEqualTo(OrderStatus.PAID)
            );
        }
    }

    @DisplayName("Callback 동시성 테스트: ")
    @Nested
    class CallbackConcurrency {

        @DisplayName("동일 Payment에 대해 callback이 10번 동시에 들어와도, 최종 상태는 정확히 1번만 반영된다.")
        @Test
        void concurrentCallbacks_idempotent() throws InterruptedException {
            // arrange
            int threadCount = 10;
            OrderInfo orderInfo = createOrderViaFacade();
            PaymentInfo requested = paymentFacade.requestPayment(orderInfo.orderId(), PaymentMethod.CARD);

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // act
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        paymentFacade.handlePgCallback(
                            requested.pgTransactionId(), true, "APPROVED"
                        );
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();
            doneLatch.await(10, TimeUnit.SECONDS);
            executorService.shutdown();

            // assert
            Payment payment = paymentRepository.findById(requested.paymentId()).orElseThrow();
            Order updatedOrder = orderRepository.findById(orderInfo.orderId()).orElseThrow();

            assertAll(
                () -> assertThat(successCount.get() + failCount.get()).as("전체 스레드 완료").isEqualTo(threadCount),
                () -> assertThat(failCount.get()).as("예외 발생 건수").isEqualTo(0),
                () -> assertThat(payment.getStatus()).as("결제 상태").isEqualTo(PaymentStatus.APPROVED),
                () -> assertThat(updatedOrder.getStatus()).as("주문 상태").isEqualTo(OrderStatus.PAID)
            );
        }
    }
}
