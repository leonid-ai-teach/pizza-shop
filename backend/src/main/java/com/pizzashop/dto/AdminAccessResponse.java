package com.pizzashop.dto;

import java.time.Instant;

public record AdminAccessResponse(
        String email,
        Instant approvedAt,
        String approvedBy
) {
}
