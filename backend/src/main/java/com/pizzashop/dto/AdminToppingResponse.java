package com.pizzashop.dto;

import java.math.BigDecimal;

public record AdminToppingResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        boolean active
) {
}
