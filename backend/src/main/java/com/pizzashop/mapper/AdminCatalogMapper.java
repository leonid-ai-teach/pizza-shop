package com.pizzashop.mapper;

import com.pizzashop.dto.AdminPizzaResponse;
import com.pizzashop.dto.AdminToppingResponse;
import com.pizzashop.entity.Pizza;
import com.pizzashop.entity.Topping;

import java.util.Comparator;
import java.util.List;

public final class AdminCatalogMapper {

    private AdminCatalogMapper() {
    }

    public static AdminToppingResponse toResponse(Topping topping) {
        return new AdminToppingResponse(
                topping.getId(),
                topping.getName(),
                topping.getDescription(),
                topping.getPrice(),
                topping.isActive());
    }

    public static AdminPizzaResponse toResponse(Pizza pizza) {
        // Unlike the public menu, inactive toppings stay visible here so an admin can see and
        // change the full assignment.
        List<AdminToppingResponse> toppings = pizza.getToppings().stream()
                .map(AdminCatalogMapper::toResponse)
                .sorted(Comparator.comparing(AdminToppingResponse::name))
                .toList();

        return new AdminPizzaResponse(
                pizza.getId(),
                pizza.getName(),
                pizza.getDescription(),
                pizza.getPrice(),
                pizza.getImagePath(),
                pizza.isActive(),
                pizza.getSortOrder(),
                toppings);
    }
}
