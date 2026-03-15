package com.loopers.domain.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    OrderItem saveItem(OrderItem orderItem);

    Optional<Order> findById(Long id);

    Optional<Order> findByIdWithLock(Long id);

    List<OrderItem> findOrderItemsByOrderId(Long orderId);
}
