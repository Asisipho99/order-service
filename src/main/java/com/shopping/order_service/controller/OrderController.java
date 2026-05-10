package com.shopping.order_service.controller;

import com.shopping.order_service.dto.*;
import com.shopping.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // POST /api/orders — place a new order
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(request));
    }

    // GET /api/orders/{orderId} — get order by ID
    // Customer can only view their own order; admin can view any
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long orderId,
                                                  Authentication auth) {
        OrderResponse order = orderService.getOrderById(orderId);

        if (!isAdmin(auth)) {
            Long tokenCustomerId = (Long) auth.getCredentials();
            if (!order.getCustomerId().equals(tokenCustomerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(order);
    }

    // GET /api/orders/customer/{customerId} — get all orders for a customer
    // Customer can only view their own orders; admin can view any customer's orders
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getByCustomer(
            @PathVariable Long customerId,
            Authentication auth) {

        if (!isAdmin(auth)) {
            Long tokenCustomerId = (Long) auth.getCredentials();
            if (!customerId.equals(tokenCustomerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    // GET /api/orders — get all orders (admin only)
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAll(Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // PUT /api/orders/{orderId}/status — update order status (admin only)
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(orderService.updateStatus(orderId, status));
    }

    // PUT /api/orders/{orderId}/cancel — cancel an order
    // Customer can only cancel their own order; admin can cancel any
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long orderId,
                                                 Authentication auth) {
        OrderResponse order = orderService.getOrderById(orderId);

        if (!isAdmin(auth)) {
            Long tokenCustomerId = (Long) auth.getCredentials();
            if (!order.getCustomerId().equals(tokenCustomerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private boolean isAdmin(Authentication auth) {
        return auth != null &&
               auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
