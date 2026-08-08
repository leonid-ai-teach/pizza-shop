package com.pizzashop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "pizza")
public class Pizza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "image_path")
    private String imagePath;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @ManyToMany
    @JoinTable(
            name = "pizza_topping",
            joinColumns = @JoinColumn(name = "pizza_id"),
            inverseJoinColumns = @JoinColumn(name = "topping_id")
    )
    private Set<Topping> toppings = new LinkedHashSet<>();

    protected Pizza() {
    }

    public Pizza(String name, String description, BigDecimal price, String imagePath,
                 boolean active, int sortOrder) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imagePath = imagePath;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    public void updateDetails(String name, String description, BigDecimal price,
                              String imagePath, int sortOrder) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imagePath = imagePath;
        this.sortOrder = sortOrder;
    }

    public void replaceToppings(Collection<Topping> newToppings) {
        toppings.clear();
        toppings.addAll(newToppings);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getImagePath() {
        return imagePath;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Set<Topping> getToppings() {
        return toppings;
    }
}
