package com.pizzashop.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Soft-delete switch. There is no hard-delete endpoint: catalog rows stay referenced by
 * historical orders (docs/Pizza_Shop_Master_Prompt.md §13).
 */
public record UpdateActiveRequest(
        @NotNull Boolean active
) {
}
