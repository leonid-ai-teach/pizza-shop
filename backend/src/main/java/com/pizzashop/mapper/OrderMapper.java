package com.pizzashop.mapper;

import com.pizzashop.dto.CustomerDataResponse;
import com.pizzashop.dto.OrderItemResponse;
import com.pizzashop.dto.OrderItemToppingResponse;
import com.pizzashop.dto.OrderResponse;
import com.pizzashop.entity.CustomerData;
import com.pizzashop.entity.Order;
import com.pizzashop.entity.OrderItem;
import com.pizzashop.entity.OrderItemTopping;

import java.util.List;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getPublicToken(),
                order.getCreatedAt(),
                order.getStatus(),
                order.getOrderType(),
                order.getPaymentStatus(),
                toCustomerDataResponse(order.getCustomerData()),
                order.getItems().stream().map(OrderMapper::toItemResponse).toList(),
                order.getTotalPrice()
        );
    }

    private static CustomerDataResponse toCustomerDataResponse(CustomerData customerData) {
        return new CustomerDataResponse(
                customerData.getFirstName(),
                customerData.getLastName(),
                customerData.getPhone(),
                customerData.getEmail(),
                customerData.getStreet(),
                customerData.getHouseNumber(),
                customerData.getPostalCode(),
                customerData.getCity()
        );
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        List<OrderItemToppingResponse> toppings = item.getToppings().stream()
                .map(OrderMapper::toItemToppingResponse)
                .toList();

        return new OrderItemResponse(
                item.getPizza().getId(),
                item.getPizza().getName(),
                item.getQuantity(),
                item.getBasePrice(),
                toppings,
                item.getItemTotalPrice()
        );
    }

    private static OrderItemToppingResponse toItemToppingResponse(OrderItemTopping topping) {
        return new OrderItemToppingResponse(
                topping.getTopping().getId(),
                topping.getToppingName(),
                topping.getToppingPrice()
        );
    }
}
