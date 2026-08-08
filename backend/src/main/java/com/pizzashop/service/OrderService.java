package com.pizzashop.service;

import com.pizzashop.dto.CreateOrderRequest;
import com.pizzashop.dto.CustomerDataRequest;
import com.pizzashop.dto.OrderItemRequest;
import com.pizzashop.dto.OrderResponse;
import com.pizzashop.entity.CustomerData;
import com.pizzashop.entity.Order;
import com.pizzashop.entity.OrderItem;
import com.pizzashop.entity.OrderItemTopping;
import com.pizzashop.entity.OrderType;
import com.pizzashop.entity.Pizza;
import com.pizzashop.entity.Topping;
import com.pizzashop.exception.ValidationException;
import com.pizzashop.exception.ResourceNotFoundException;
import com.pizzashop.mapper.OrderMapper;
import com.pizzashop.repository.OrderRepository;
import com.pizzashop.repository.PizzaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final PizzaRepository pizzaRepository;

    public OrderService(OrderRepository orderRepository, PizzaRepository pizzaRepository) {
        this.orderRepository = orderRepository;
        this.pizzaRepository = pizzaRepository;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        validateAddress(request.orderType(), request.customerData());

        Long orderNumber = orderRepository.nextOrderNumber();
        CustomerData customerData = toCustomerData(request.customerData());
        Order order = new Order(orderNumber, request.orderType(), customerData);

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            OrderItem item = buildOrderItem(itemRequest);
            order.addItem(item);
            totalPrice = totalPrice.add(item.getItemTotalPrice());
        }
        order.updateTotalPrice(totalPrice);

        Order saved = orderRepository.save(order);
        return OrderMapper.toResponse(saved);
    }

    /**
     * Looks an order up by its unguessable public token. Deliberately not by primary key: this
     * endpoint is unauthenticated, and sequential ids would let anyone enumerate other
     * customers' orders and their contact and delivery details.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByPublicToken(String publicToken) {
        Order order = orderRepository.findByPublicToken(publicToken)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        return OrderMapper.toResponse(order);
    }

    private void validateAddress(OrderType orderType, CustomerDataRequest customerData) {
        if (orderType != OrderType.DELIVERY) {
            return;
        }
        if (!StringUtils.hasText(customerData.street()) || !StringUtils.hasText(customerData.houseNumber())
                || !StringUtils.hasText(customerData.postalCode()) || !StringUtils.hasText(customerData.city())) {
            throw new ValidationException(
                    "Street, house number, postal code and city are required for delivery orders.");
        }
    }

    private OrderItem buildOrderItem(OrderItemRequest itemRequest) {
        Pizza pizza = pizzaRepository.findById(itemRequest.pizzaId())
                .filter(Pizza::isActive)
                .orElseThrow(() -> new ValidationException("Invalid or inactive pizza: " + itemRequest.pizzaId()));

        BigDecimal basePrice = pizza.getPrice();
        // A pizza's own toppings collection already carries active status and the
        // pizza-topping link in one place, so resolving against it in memory covers
        // "exists", "active" and "linked to this pizza" without extra queries.
        Set<Long> requestedToppingIds = new LinkedHashSet<>(itemRequest.toppingIds());
        List<Topping> toppings = requestedToppingIds.stream()
                .map(toppingId -> resolveToppingForPizza(pizza, toppingId))
                .toList();
        BigDecimal toppingsTotal = toppings.stream()
                .map(Topping::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unitPrice = basePrice.add(toppingsTotal);
        BigDecimal itemTotalPrice = unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity()));

        OrderItem item = new OrderItem(pizza, itemRequest.quantity(), basePrice, itemTotalPrice);
        for (Topping topping : toppings) {
            item.addTopping(new OrderItemTopping(topping, topping.getName(), topping.getPrice()));
        }
        return item;
    }

    private Topping resolveToppingForPizza(Pizza pizza, Long toppingId) {
        return pizza.getToppings().stream()
                .filter(topping -> topping.getId().equals(toppingId) && topping.isActive())
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "Invalid, inactive or unavailable topping " + toppingId + " for pizza " + pizza.getId()));
    }

    private CustomerData toCustomerData(CustomerDataRequest request) {
        return new CustomerData(
                request.firstName(), request.lastName(), request.phone(), request.email(),
                request.street(), request.houseNumber(), request.postalCode(), request.city()
        );
    }
}
