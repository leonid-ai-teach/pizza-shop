package com.pizzashop.entity;

import java.util.Set;

/**
 * Order lifecycle. {@code DONE} and {@code CANCELLED} are terminal; cancelling is only possible
 * while an order is still open (docs/Pizza_Shop_Master_Prompt.md §12).
 */
public enum OrderStatus {
    NEW,
    IN_PROGRESS,
    DONE,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        return allowedTargets().contains(target);
    }

    public boolean isTerminal() {
        return allowedTargets().isEmpty();
    }

    private Set<OrderStatus> allowedTargets() {
        return switch (this) {
            case NEW -> Set.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> Set.of(DONE, CANCELLED);
            case DONE, CANCELLED -> Set.of();
        };
    }
}
