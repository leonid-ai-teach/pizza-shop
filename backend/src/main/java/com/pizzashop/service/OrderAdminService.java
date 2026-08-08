package com.pizzashop.service;

import com.pizzashop.dto.OrderResponse;
import com.pizzashop.dto.OrderSummaryResponse;
import com.pizzashop.entity.Order;
import com.pizzashop.entity.OrderStatus;
import com.pizzashop.exception.ResourceNotFoundException;
import com.pizzashop.exception.ValidationException;
import com.pizzashop.mapper.OrderMapper;
import com.pizzashop.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderAdminService {

    private final OrderRepository orderRepository;

    public OrderAdminService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(OrderStatus status) {
        List<Order> orders = status == null
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByStatusOrderByCreatedAtDesc(status);
        return orders.stream().map(OrderMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return OrderMapper.toResponse(findOrder(id));
    }

    @Transactional(readOnly = true)
    public OrderSummaryResponse getSummary() {
        return new OrderSummaryResponse(
                orderRepository.countByStatus(OrderStatus.NEW),
                orderRepository.countByStatus(OrderStatus.IN_PROGRESS),
                orderRepository.countByStatus(OrderStatus.DONE));
    }

    public OrderResponse updateStatus(Long id, OrderStatus target) {
        Order order = findOrder(id);
        try {
            order.changeStatusTo(target);
        } catch (IllegalStateException ex) {
            // The entity guards the lifecycle; surface it as a client error rather than a 500.
            throw new ValidationException(ex.getMessage());
        }
        return OrderMapper.toResponse(orderRepository.save(order));
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }
}
