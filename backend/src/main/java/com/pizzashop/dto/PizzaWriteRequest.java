package com.pizzashop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Create and update carry identical fields, so they share one request type. {@code toppingIds}
 * is the complete set of toppings offerable on this pizza; the service reconciles the mapping
 * rows to match (docs/specs/admin-area.md).
 */
public record PizzaWriteRequest(
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        String imagePath,
        @PositiveOrZero int sortOrder,
        @NotNull List<@NotNull Long> toppingIds
) {
}
