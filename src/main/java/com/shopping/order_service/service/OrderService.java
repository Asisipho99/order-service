package com.shopping.order_service.service;

import com.shopping.order_service.dto.*;
import com.shopping.order_service.entity.Order;
import com.shopping.order_service.entity.OrderItem;
import com.shopping.order_service.enums.OrderStatus;
import com.shopping.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository            orderRepository;
    private final StatusNotificationService  notificationService;

    // Place a new order
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {

        // Calculate total amount from all items
        BigDecimal total = request.getItems().stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create the order entity
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);

        // Map request items to order item entities
        List<OrderItem> orderItems = request.getItems().stream().map(itemReq -> {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(itemReq.getProductId());
            item.setProductName(itemReq.getProductName());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            return item;
        }).collect(Collectors.toList());

        order.setItems(orderItems);
        Order savedOrder = orderRepository.save(order);

        return mapToResponse(savedOrder);
    }

    // Get a single order by ID
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return mapToResponse(order);
    }

    // Get all orders for a customer
    public List<OrderResponse> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Get all orders (admin)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllWithItems()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Update order status (admin)
    @Transactional
    public OrderResponse updateStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Invalid status. Must be one of: PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED");
        }

        OrderStatus current = order.getStatus();
        if (!current.canTransitionTo(newStatus)) {
            throw new RuntimeException(
                    "Cannot transition order from " + current
                    + " to " + newStatus
                    + ". Allowed transitions: " + current.allowedTransitions());
        }

        order.setStatus(newStatus);
        OrderResponse response = mapToResponse(orderRepository.save(order));

        // Notify customer of the status change asynchronously
        notificationService.sendStatusUpdateEmail(response);

        return response;
    }

    // Cancel an order
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.SHIPPED ||
                order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException(
                    "Cannot cancel an order that has already been shipped or delivered");
        }

        order.setStatus(OrderStatus.CANCELLED);
        OrderResponse response = mapToResponse(orderRepository.save(order));

        // Notify customer of cancellation asynchronously
        notificationService.sendStatusUpdateEmail(response);

        return response;
    }

    // Map Order entity to OrderResponse DTO
    private OrderResponse mapToResponse(Order order) {
        List<OrderResponse.OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderResponse.OrderItemDto(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice()))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getOrderId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                itemDtos);
    }
}
