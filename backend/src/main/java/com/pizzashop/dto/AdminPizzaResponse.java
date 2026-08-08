package com.pizzashop.dto;

import java.math.BigDecimal;
import java.util.List;

/** Like {@link PizzaResponse} but exposes {@code active}, which the public menu never needs. */
public record AdminPizzaResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imagePath,
        boolean active,
        int sortOrder,
        List<AdminToppingResponse> toppings
) {
}
