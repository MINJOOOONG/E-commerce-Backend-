package com.loopers.application.order.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderCreatedEvent 이벤트 인프라를 End-to-End로 검증하는 테스트.
 *
 * <p>@SpringBootTest 없이 경량 ApplicationContext를 직접 구성하여
 * Docker/Testcontainers 의존 없이 이벤트 발행 → 핸들러 수신 흐름을 검증한다.</p>
 *
 * <p>OrderFacade 소스가 복원되면, 아래 코드 2줄로 실제 주문 흐름에 연결 가능:</p>
 * <pre>
 * // OrderFacade 필드 추가
 * private final ApplicationEventPublisher eventPublisher;
 *
 * // createOrder() return 직전
 * eventPublisher.publishEvent(new OrderCreatedEvent(
 *     savedOrder.getId(), userId, savedOrder.getTotalAmount()
 * ));
 * </pre>
 */
class OrderEventIntegrationTest {

    @DisplayName("이벤트 발행 → 핸들러 수신 통합 테스트")
    @Nested
    class EventFlow {

        @DisplayName("publishEvent 호출 시, 핸들러가 이벤트를 수신한다.")
        @Test
        void handlerReceivesPublishedEvent() {
            // arrange
            try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestEventConfig.class)) {

                TestOrderEventListener listener = context.getBean(TestOrderEventListener.class);
                OrderCreatedEvent event = new OrderCreatedEvent(1L, 100L, 50000L, 10L);

                // act — context 자체가 ApplicationEventPublisher
                context.publishEvent(event);

                // assert
                assertThat(listener.getReceivedEvent()).isNotNull();
                assertThat(listener.getReceivedEvent().getOrderId()).isEqualTo(1L);
                assertThat(listener.getReceivedEvent().getUserId()).isEqualTo(100L);
                assertThat(listener.getReceivedEvent().getTotalAmount()).isEqualTo(50000L);
                assertThat(listener.getReceivedEvent().getCouponId()).isEqualTo(10L);
            }
        }

        @DisplayName("couponId가 null인 이벤트도 핸들러가 정상 수신한다.")
        @Test
        void handlerReceivesEvent_withoutCouponId() {
            // arrange
            try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestEventConfig.class)) {

                TestOrderEventListener listener = context.getBean(TestOrderEventListener.class);
                OrderCreatedEvent event = new OrderCreatedEvent(2L, 200L, 30000L);

                // act
                context.publishEvent(event);

                // assert
                assertThat(listener.getReceivedEvent()).isNotNull();
                assertThat(listener.getReceivedEvent().getOrderId()).isEqualTo(2L);
                assertThat(listener.getReceivedEvent().getCouponId()).isNull();
            }
        }

        @DisplayName("비동기 이벤트 핸들러가 별도 스레드에서 실행된다.")
        @Test
        void asyncHandlerRunsOnSeparateThread() throws InterruptedException {
            // arrange
            try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestAsyncEventConfig.class)) {

                TestAsyncOrderEventListener listener = context.getBean(TestAsyncOrderEventListener.class);
                OrderCreatedEvent event = new OrderCreatedEvent(3L, 300L, 70000L);

                // act
                context.publishEvent(event);
                boolean completed = listener.awaitCompletion(3, TimeUnit.SECONDS);

                // assert
                assertThat(completed).isTrue();
                assertThat(listener.getReceivedEvent()).isNotNull();
                assertThat(listener.getReceivedEvent().getOrderId()).isEqualTo(3L);
                assertThat(listener.getExecutionThreadName()).isNotEqualTo(Thread.currentThread().getName());
                assertThat(listener.getExecutionThreadName()).startsWith("test-event-");
            }
        }
    }

    // --- 테스트용 Configuration ---

    @Configuration
    static class TestEventConfig {

        @Bean
        TestOrderEventListener testOrderEventListener() {
            return new TestOrderEventListener();
        }
    }

    /**
     * 동기 @EventListener — 이벤트 발행 → 수신 흐름 검증용
     */
    static class TestOrderEventListener {

        private OrderCreatedEvent receivedEvent;

        @org.springframework.context.event.EventListener
        public void handle(OrderCreatedEvent event) {
            this.receivedEvent = event;
        }

        public OrderCreatedEvent getReceivedEvent() {
            return receivedEvent;
        }
    }

    @Configuration
    @EnableAsync
    static class TestAsyncEventConfig {

        @Bean
        TestAsyncOrderEventListener testAsyncOrderEventListener() {
            return new TestAsyncOrderEventListener();
        }

        @Bean(name = "testEventExecutor")
        public java.util.concurrent.Executor testEventExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setThreadNamePrefix("test-event-");
            executor.initialize();
            return executor;
        }
    }

    /**
     * 비동기 @EventListener — @Async가 별도 스레드에서 실행되는지 검증용
     */
    static class TestAsyncOrderEventListener {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<OrderCreatedEvent> receivedEvent = new AtomicReference<>();
        private final AtomicReference<String> executionThreadName = new AtomicReference<>();

        @org.springframework.scheduling.annotation.Async("testEventExecutor")
        @org.springframework.context.event.EventListener
        public void handle(OrderCreatedEvent event) {
            this.receivedEvent.set(event);
            this.executionThreadName.set(Thread.currentThread().getName());
            latch.countDown();
        }

        public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        public OrderCreatedEvent getReceivedEvent() {
            return receivedEvent.get();
        }

        public String getExecutionThreadName() {
            return executionThreadName.get();
        }
    }
}
