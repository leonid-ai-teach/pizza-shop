package com.pizzashop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item_topping")
public class OrderItemTopping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne
    @JoinColumn(name = "topping_id", nullable = false)
    private Topping topping;

    @Column(name = "topping_name", nullable = false)
    private String toppingName;

    @Column(name = "topping_price", nullable = false)
    private BigDecimal toppingPrice;

    protected OrderItemTopping() {
    }

    public OrderItemTopping(Topping topping, String toppingName, BigDecimal toppingPrice) {
        this.topping = topping;
        this.toppingName = toppingName;
        this.toppingPrice = toppingPrice;
    }

    void assignToOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }

    public Long getId() {
        return id;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public Topping getTopping() {
        return topping;
    }

    public String getToppingName() {
        return toppingName;
    }

    public BigDecimal getToppingPrice() {
        return toppingPrice;
    }
}
