package com.pizzashop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private Long orderNumber;

    /**
     * Unguessable handle for the public confirmation page. The sequential id must never appear
     * in a public URL, or anyone could enumerate other customers' orders and their contact and
     * delivery details.
     */
    @Column(name = "public_token", nullable = false, unique = true, updatable = false)
    private String publicToken;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Embedded
    private CustomerData customerData;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    /** Makes concurrent status changes fail loudly instead of silently overwriting each other. */
    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(Long orderNumber, OrderType orderType, CustomerData customerData) {
        this.orderNumber = orderNumber;
        this.orderType = orderType;
        this.customerData = customerData;
        this.status = OrderStatus.NEW;
        this.paymentStatus = PaymentStatus.NOT_REQUIRED;
        this.createdAt = Instant.now();
        this.totalPrice = BigDecimal.ZERO;
        this.publicToken = UUID.randomUUID().toString();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.assignToOrder(this);
    }

    public void updateTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    /**
     * Moves the order to {@code target}, refusing any transition the lifecycle disallows so an
     * order can never be resurrected from a terminal status or skip a step.
     */
    public void changeStatusTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Cannot change order status from " + status + " to " + target + ".");
        }
        this.status = target;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderNumber() {
        return orderNumber;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public CustomerData getCustomerData() {
        return customerData;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
