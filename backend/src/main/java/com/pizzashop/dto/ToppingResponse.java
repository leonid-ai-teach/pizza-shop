package com.pizzashop.dto;

import java.math.BigDecimal;

public record ToppingResponse(
        Long id,
        String name,
        String description,
        BigDecimal price
) {
}
