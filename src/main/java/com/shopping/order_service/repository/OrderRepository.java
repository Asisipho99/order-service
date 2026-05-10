package com.shopping.order_service.repository;

import com.shopping.order_service.entity.Order;
import com.shopping.order_service.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // JOIN FETCH loads items in the same query — eliminates N+1
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.customerId = :customerId")
    List<Order> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items")
    List<Order> findAllWithItems();

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);

    // Get orders by status
    List<Order> findByStatus(OrderStatus status);

    // Get orders for a customer filtered by status
    List<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);
}
