package com.pizzashop.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemResponse(
        Long pizzaId,
        String pizzaName,
        int quantity,
        BigDecimal basePrice,
        List<OrderItemToppingResponse> toppings,
        BigDecimal itemTotalPrice
) {
}
