package com.pizzashop.controller;

import com.pizzashop.dto.OrderResponse;
import com.pizzashop.dto.OrderSummaryResponse;
import com.pizzashop.dto.UpdateOrderStatusRequest;
import com.pizzashop.entity.OrderStatus;
import com.pizzashop.service.OrderAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class OrderAdminController {

    private final OrderAdminService orderAdminService;

    public OrderAdminController(OrderAdminService orderAdminService) {
        this.orderAdminService = orderAdminService;
    }

    @GetMapping
    public List<OrderResponse> getOrders(@RequestParam(required = false) OrderStatus status) {
        return orderAdminService.getOrders(status);
    }

    @GetMapping("/summary")
    public OrderSummaryResponse getSummary() {
        return orderAdminService.getSummary();
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderAdminService.getOrder(id);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id,
                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderAdminService.updateStatus(id, request.status());
    }
}
