package com.pizzashop.dto;

import java.math.BigDecimal;
import java.util.List;

public record PizzaResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imagePath,
        int sortOrder,
        List<ToppingResponse> toppings
) {
}
