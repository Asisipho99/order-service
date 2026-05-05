package com.shopping.order_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {

    @NotNull
    private Long customerId;

    private String customerEmail;

    @NotEmpty
    private List<OrderItemRequest> items;
}
