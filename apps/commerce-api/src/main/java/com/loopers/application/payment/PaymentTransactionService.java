package com.loopers.application.payment;

import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class PaymentTransactionService {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public Payment createPendingPayment(Long orderId, PaymentMethod method) {
        Order order = orderRepository.findByIdWithLock(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다"));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new CoreException(ErrorType.CONFLICT, "결제 대기 상태의 주문만 결제할 수 있습니다");
        }

        Payment payment = new Payment(orderId, order.getUserId(), order.getFinalPrice(), method);
        return paymentService.createPayment(payment);
    }

    @Transactional
    public Payment assignPgTransactionId(Long paymentId, String pgTransactionId) {
        Payment payment = paymentService.getPayment(paymentId);
        payment.assignPgTransactionId(pgTransactionId);
        paymentService.createPayment(payment);
        return payment;
    }

    @Transactional
    public Payment approvePayment(Long paymentId, String pgTransactionId, String responseCode) {
        Payment payment = paymentService.getPayment(paymentId);
        payment.approve(pgTransactionId, responseCode);
        paymentService.createPayment(payment);

        Order order = orderRepository.findByIdWithLock(payment.getOrderId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다"));
        order.markPaid();
        orderRepository.save(order);

        return payment;
    }

    @Transactional
    public Payment failPaymentWithCompensation(Long paymentId, String responseCode) {
        Payment payment = paymentService.getPayment(paymentId);
        payment.fail(responseCode);
        paymentService.createPayment(payment);

        Order order = orderRepository.findByIdWithLock(payment.getOrderId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다"));
        order.cancel();
        orderRepository.save(order);

        compensate(order);

        return payment;
    }

    private void compensate(Order order) {
        List<OrderItem> orderItems = orderRepository.findOrderItemsByOrderId(order.getId());
        for (OrderItem item : orderItems) {
            Product product = productRepository.findByIdWithLock(item.getProductId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다"));
            product.increaseStock(item.getQuantity());
            productRepository.save(product);
        }

        User user = userRepository.findByIdWithLock(order.getUserId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다"));
        user.refundPoint(order.getFinalPrice());
        userRepository.save(user);

        if (order.getCouponId() != null) {
            UserCoupon coupon = userCouponRepository.findByIdWithLock(order.getCouponId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다"));
            coupon.restore();
            userCouponRepository.save(coupon);
        }
    }
}
