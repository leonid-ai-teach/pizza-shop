package com.pizzashop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ToppingWriteRequest(
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin(value = "0.0") BigDecimal price
) {
}
