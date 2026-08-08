package com.pizzashop.dto;

public record OrderSummaryResponse(
        long newCount,
        long inProgressCount,
        long doneCount
) {
}
