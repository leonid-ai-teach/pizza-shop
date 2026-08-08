package com.pizzashop.dto;

import com.pizzashop.entity.OrderStatus;
import com.pizzashop.entity.OrderType;
import com.pizzashop.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long orderNumber,
        Instant createdAt,
        OrderStatus status,
        OrderType orderType,
        PaymentStatus paymentStatus,
        CustomerDataResponse customerData,
        List<OrderItemResponse> items,
        BigDecimal totalPrice
) {
}
