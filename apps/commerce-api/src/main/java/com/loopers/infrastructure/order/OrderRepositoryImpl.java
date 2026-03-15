package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderItemJpaRepository orderItemJpaRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public OrderItem saveItem(OrderItem orderItem) {
        return orderItemJpaRepository.save(orderItem);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findById(id);
    }

    @Override
    public Optional<Order> findByIdWithLock(Long id) {
        return orderJpaRepository.findByIdWithLock(id);
    }

    @Override
    public List<OrderItem> findOrderItemsByOrderId(Long orderId) {
        return orderItemJpaRepository.findByOrderId(orderId);
    }
}
