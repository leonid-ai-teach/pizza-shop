package com.pizzashop.mapper;

import com.pizzashop.dto.PizzaResponse;
import com.pizzashop.dto.ToppingResponse;
import com.pizzashop.entity.Pizza;
import com.pizzashop.entity.Topping;

import java.util.Comparator;
import java.util.List;

public final class PizzaMapper {

    private PizzaMapper() {
    }

    public static PizzaResponse toResponse(Pizza pizza) {
        List<ToppingResponse> toppings = pizza.getToppings().stream()
                .filter(Topping::isActive)
                .map(ToppingMapper::toResponse)
                .sorted(Comparator.comparing(ToppingResponse::name))
                .toList();

        return new PizzaResponse(
                pizza.getId(),
                pizza.getName(),
                pizza.getDescription(),
                pizza.getPrice(),
                pizza.getImagePath(),
                pizza.getSortOrder(),
                toppings
        );
    }
}
