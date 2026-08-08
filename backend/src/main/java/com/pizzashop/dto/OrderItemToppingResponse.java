package com.pizzashop.dto;

import java.math.BigDecimal;

public record OrderItemToppingResponse(
        Long toppingId,
        String name,
        BigDecimal price
) {
}
