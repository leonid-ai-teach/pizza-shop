package com.pizzashop;

import com.pizzashop.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exhaustive coverage of the status state machine lives here rather than in the API test:
 * enumerating every (from, to) pair through MockMvc round-trips would be repetitive without
 * testing anything extra.
 */
class OrderStatusTransitionTest {

    @ParameterizedTest
    @CsvSource({
            "NEW, IN_PROGRESS",
            "NEW, CANCELLED",
            "IN_PROGRESS, DONE",
            "IN_PROGRESS, CANCELLED",
    })
    void allowsTheLegalTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "NEW, DONE",
            "NEW, NEW",
            "IN_PROGRESS, NEW",
            "IN_PROGRESS, IN_PROGRESS",
            "DONE, NEW",
            "DONE, IN_PROGRESS",
            "DONE, CANCELLED",
            "DONE, DONE",
            "CANCELLED, NEW",
            "CANCELLED, IN_PROGRESS",
            "CANCELLED, DONE",
            "CANCELLED, CANCELLED",
    })
    void rejectsEveryOtherTransition(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"DONE", "CANCELLED"})
    void terminalStatusesHaveNoOnwardTransitions(OrderStatus terminal) {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(terminal.canTransitionTo(target)).isFalse();
        }
        assertThat(terminal.isTerminal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"NEW", "IN_PROGRESS"})
    void openStatusesAreNotTerminal(OrderStatus open) {
        assertThat(open.isTerminal()).isFalse();
    }
}
