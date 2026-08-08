package com.pizzashop.dto;

import com.pizzashop.entity.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull OrderType orderType,
        @NotNull @Valid CustomerDataRequest customerData,
        @NotEmpty List<@NotNull @Valid OrderItemRequest> items
) {
}
