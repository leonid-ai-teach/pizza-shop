package com.pizzashop.mapper;

import com.pizzashop.dto.ToppingResponse;
import com.pizzashop.entity.Topping;

public final class ToppingMapper {

    private ToppingMapper() {
    }

    public static ToppingResponse toResponse(Topping topping) {
        return new ToppingResponse(
                topping.getId(),
                topping.getName(),
                topping.getDescription(),
                topping.getPrice()
        );
    }
}
