package com.loopers.domain.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FakeOrderRepository implements OrderRepository {

    private final List<Order> orders = new ArrayList<>();
    private final List<OrderItem> items = new ArrayList<>();
    private long orderSequence = 1L;
    private long itemSequence = 1L;

    @Override
    public Order save(Order order) {
        orders.add(order);
        return order;
    }

    @Override
    public OrderItem saveItem(OrderItem orderItem) {
        items.add(orderItem);
        return orderItem;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orders.stream()
            .filter(o -> o.getId().equals(id))
            .findFirst();
    }

    @Override
    public Optional<Order> findByIdWithLock(Long id) {
        return findById(id);
    }

    @Override
    public List<OrderItem> findOrderItemsByOrderId(Long orderId) {
        return items.stream()
            .filter(i -> orderId.equals(i.getOrderId()))
            .toList();
    }

    public List<Order> getOrders() {
        return orders;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
