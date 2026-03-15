package com.loopers.application.payment;

import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentFacadeUnitTest {

    private PaymentFacade paymentFacade;
    private FakePgClient fakePgClient;
    private SimpleOrderRepository orderRepository;
    private SimpleProductRepository productRepository;
    private SimpleUserRepository userRepository;
    private SimpleUserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        orderRepository = new SimpleOrderRepository();
        productRepository = new SimpleProductRepository();
        userRepository = new SimpleUserRepository();
        userCouponRepository = new SimpleUserCouponRepository();
        FakePaymentRepository paymentRepository = new FakePaymentRepository();
        PaymentService paymentService = new PaymentService(paymentRepository);
        fakePgClient = new FakePgClient();

        PaymentTransactionService txService = new PaymentTransactionService(
            paymentService, orderRepository, productRepository, userRepository, userCouponRepository
        );
        paymentFacade = new PaymentFacade(txService, paymentService, fakePgClient);
    }

    private Order createOrderWithItems(Long userId, Long productId) {
        Product product = new Product(1L, "상품A", 10000L, "설명", 10);
        productRepository.addWithId(productId, product);

        User user = new User("테스트유저", 100000L);
        userRepository.addWithId(userId, user);

        OrderItem item = new OrderItem(productId, "상품A", 10000L, 2);
        Order order = new Order(userId, List.of(item));

        product.decreaseStock(2);
        user.deductPoint(order.getFinalPrice());

        OrderService orderService = new OrderService(orderRepository);
        orderService.createOrder(order);

        return order;
    }

    @DisplayName("결제를 요청할 때,")
    @Nested
    class RequestPayment {

        @DisplayName("PG 응답이 성공이면, Payment는 PENDING 상태로 유지된다.")
        @Test
        void paymentRemainsPending_whenPgSucceeds() {
            // arrange
            Order order = createOrderWithItems(1L, 1L);

            // act
            PaymentInfo result = paymentFacade.requestPayment(order.getId(), PaymentMethod.CARD);

            // assert
            assertAll(
                () -> assertThat(result.status()).isEqualTo(PaymentStatus.PENDING),
                () -> assertThat(result.orderId()).isEqualTo(order.getId()),
                () -> assertThat(result.amount()).isEqualTo(20000L),
                () -> assertThat(result.method()).isEqualTo(PaymentMethod.CARD)
            );
        }

        @DisplayName("PG 즉시 실패이면, Payment는 FAILED 상태이고 보상 처리된다.")
        @Test
        void paymentFails_whenPgImmediatelyFails() {
            // arrange
            Order order = createOrderWithItems(1L, 1L);
            fakePgClient.setShouldFail(true);

            // act
            PaymentInfo result = paymentFacade.requestPayment(order.getId(), PaymentMethod.CARD);

            // assert
            assertAll(
                () -> assertThat(result.status()).isEqualTo(PaymentStatus.FAILED),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED),
                () -> assertThat(userRepository.findById(1L).get().getPoint())
                    .as("포인트 환불").isEqualTo(100000L),
                () -> assertThat(productRepository.findById(1L).get().getStockQuantity().value())
                    .as("재고 복구").isEqualTo(10)
            );
        }

        @DisplayName("PG 호출 중 예외가 발생하면, Payment는 PENDING 상태로 유지된다.")
        @Test
        void paymentRemainsPending_whenPgThrowsException() {
            // arrange
            Order order = createOrderWithItems(1L, 1L);
            fakePgClient.setShouldThrow(true);

            // act
            PaymentInfo result = paymentFacade.requestPayment(order.getId(), PaymentMethod.CARD);

            // assert
            assertAll(
                () -> assertThat(result.status()).isEqualTo(PaymentStatus.PENDING),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT)
            );
        }

        @DisplayName("주문이 PENDING_PAYMENT 상태가 아니면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflict_whenOrderNotPendingPayment() {
            // arrange
            Order order = createOrderWithItems(1L, 1L);
            order.markPaid();

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                paymentFacade.requestPayment(order.getId(), PaymentMethod.CARD));

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("PG callback을 처리할 때,")
    @Nested
    class HandlePgCallback {

        @DisplayName("성공 callback이면, Payment는 APPROVED, Order는 PAID가 된다.")
        @Test
        void approvesPayment_whenCallbackSuccess() {
            // arrange
            Order order = createOrderWithItems(1L, 1L);
            PaymentInfo requested = paymentFacade.requestPayment(order.getId(), PaymentMethod.CARD);

            // act
            PaymentInfo result = paymentFacade.handlePgCallback(
                requested.pgTransactionId(), true, "APPROVED"
            );

            // assert
            assertAll(
                () -> assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID)
            );
        }

        @DisplayName("실패 callback이면, Payment는 FAILED, Order는 CANCELLED이고 보상 처리된다.")
        @Test
        void failsPayment_whenCallbackFails() {
            // arrange
            Order order = createOrderWithItems(1L, 1L);
            PaymentInfo requested = paymentFacade.requestPayment(order.getId(), PaymentMethod.CARD);

            // act
            PaymentInfo result = paymentFacade.handlePgCallback(
                requested.pgTransactionId(), false, "CARD_DECLINED"
            );

            // assert
            assertAll(
                () -> assertThat(result.status()).isEqualTo(PaymentStatus.FAILED),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED),
                () -> assertThat(userRepository.findById(1L).get().getPoint())
                    .as("포인트 환불").isEqualTo(100000L),
                () -> assertThat(productRepository.findById(1L).get().getStockQuantity().value())
                    .as("재고 복구").isEqualTo(10)
            );
        }

        @DisplayName("이미 APPROVED 상태이면, 중복 callback을 무시하고 현재 상태를 반환한다.")
        @Test
        void ignoresCallback_whenAlreadyApproved() {
            // arrange
            Order order = createOrderWithItems(1L, 1L);
            PaymentInfo requested = paymentFacade.requestPayment(order.getId(), PaymentMethod.CARD);
            paymentFacade.handlePgCallback(requested.pgTransactionId(), true, "APPROVED");

            // act
            PaymentInfo result = paymentFacade.handlePgCallback(
                requested.pgTransactionId(), true, "APPROVED"
            );

            // assert
            assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        }

        @DisplayName("이미 FAILED 상태이면, 중복 callback을 무시하고 현재 상태를 반환한다.")
        @Test
        void ignoresCallback_whenAlreadyFailed() {
            // arrange
            Order order = createOrderWithItems(1L, 1L);
            PaymentInfo requested = paymentFacade.requestPayment(order.getId(), PaymentMethod.CARD);
            paymentFacade.handlePgCallback(requested.pgTransactionId(), false, "CARD_DECLINED");

            // act
            PaymentInfo result = paymentFacade.handlePgCallback(
                requested.pgTransactionId(), true, "APPROVED"
            );

            // assert
            assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    // --- 테스트용 Fake Repository (package-private) ---

    static class SimpleOrderRepository implements OrderRepository {
        private final Map<Long, Order> store = new HashMap<>();
        private final Map<Long, OrderItem> itemStore = new HashMap<>();
        private long itemSeq = 1L;

        @Override
        public Order save(Order order) {
            store.put(order.getId(), order);
            return order;
        }

        @Override
        public OrderItem saveItem(OrderItem orderItem) {
            itemStore.put(itemSeq++, orderItem);
            return orderItem;
        }

        @Override
        public Optional<Order> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Order> findByIdWithLock(Long id) {
            return findById(id);
        }

        @Override
        public List<OrderItem> findOrderItemsByOrderId(Long orderId) {
            return itemStore.values().stream()
                .filter(i -> orderId.equals(i.getOrderId()))
                .toList();
        }
    }

    static class SimpleProductRepository implements ProductRepository {
        private final Map<Long, Product> store = new HashMap<>();

        void addWithId(Long id, Product product) {
            store.put(id, product);
        }

        @Override
        public Product save(Product product) { return product; }

        @Override
        public Optional<Product> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Product> findByIdWithLock(Long id) { return findById(id); }

        @Override
        public boolean existsById(Long id) { return store.containsKey(id); }

        @Override
        public org.springframework.data.domain.Page<Product> findAll(org.springframework.data.domain.Pageable pageable) {
            return org.springframework.data.domain.Page.empty();
        }

        @Override
        public org.springframework.data.domain.Page<Product> findByBrandId(Long brandId, org.springframework.data.domain.Pageable pageable) {
            return org.springframework.data.domain.Page.empty();
        }
    }

    static class SimpleUserRepository implements UserRepository {
        private final Map<Long, User> store = new HashMap<>();

        void addWithId(Long id, User user) { store.put(id, user); }

        @Override
        public Optional<User> findById(Long id) { return Optional.ofNullable(store.get(id)); }

        @Override
        public Optional<User> findByIdWithLock(Long id) { return findById(id); }

        @Override
        public User save(User user) { return user; }
    }

    static class SimpleUserCouponRepository implements UserCouponRepository {
        private final Map<Long, UserCoupon> store = new HashMap<>();

        void addWithId(Long id, UserCoupon coupon) { store.put(id, coupon); }

        @Override
        public UserCoupon save(UserCoupon userCoupon) { return userCoupon; }

        @Override
        public Optional<UserCoupon> findById(Long id) { return Optional.ofNullable(store.get(id)); }

        @Override
        public Optional<UserCoupon> findByIdWithLock(Long id) { return findById(id); }

        @Override
        public List<UserCoupon> findByUserId(Long userId) {
            return store.values().stream().filter(uc -> uc.getUserId().equals(userId)).toList();
        }
    }
}
