package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderService {

    private final OrderRepository orderRepository;

    public Order createOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        for (OrderItem item : order.getOrderItems()) {
            item.assignOrderId(savedOrder.getId());
            orderRepository.saveItem(item);
        }
        return savedOrder;
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다"));
    }

    public List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        return orderRepository.findOrderItemsByOrderId(orderId);
    }
}
