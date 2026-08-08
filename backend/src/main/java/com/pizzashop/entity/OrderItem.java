package com.pizzashop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "pizza_id", nullable = false)
    private Pizza pizza;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "item_total_price", nullable = false)
    private BigDecimal itemTotalPrice;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItemTopping> toppings = new ArrayList<>();

    protected OrderItem() {
    }

    public OrderItem(Pizza pizza, int quantity, BigDecimal basePrice, BigDecimal itemTotalPrice) {
        this.pizza = pizza;
        this.quantity = quantity;
        this.basePrice = basePrice;
        this.itemTotalPrice = itemTotalPrice;
    }

    void assignToOrder(Order order) {
        this.order = order;
    }

    public void addTopping(OrderItemTopping topping) {
        toppings.add(topping);
        topping.assignToOrderItem(this);
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Pizza getPizza() {
        return pizza;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public BigDecimal getItemTotalPrice() {
        return itemTotalPrice;
    }

    public List<OrderItemTopping> getToppings() {
        return toppings;
    }
}
