package com.shopping.order_service.repository;

import com.shopping.order_service.entity.Order;
import com.shopping.order_service.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Get all orders for a specific customer
    List<Order> findByCustomerId(Long customerId);

    // Get orders by status
    List<Order> findByStatus(OrderStatus status);

    // Get orders for a customer filtered by status
    List<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);
}
