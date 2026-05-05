package com.shopping.order_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemRequest {

    @NotNull
    private Long productId;

    @NotBlank
    private String productName;

    @Min(1)
    private Integer quantity;

    @NotNull
    private BigDecimal unitPrice;
}
