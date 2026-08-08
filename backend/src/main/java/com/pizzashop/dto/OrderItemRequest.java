package com.pizzashop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderItemRequest(
        @NotNull Long pizzaId,
        @Min(1) int quantity,
        @NotNull List<@NotNull Long> toppingIds
) {
}
