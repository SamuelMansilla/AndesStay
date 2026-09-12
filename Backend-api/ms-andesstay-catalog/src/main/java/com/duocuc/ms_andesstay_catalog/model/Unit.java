package com.duocuc.ms_andesstay_catalog.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "units")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // "HABITACION" o "CABAÑA"

    @Column(nullable = false)
    private Integer capacity; // Cupos

    @Column(nullable = false)
    private BigDecimal price; // Tarifa

    @Column(nullable = false)
    private Boolean available; // Disponibilidad

    // Constructores vacíos y con parámetros necesarios para JPA
    public Unit() {}

    public Unit(String name, String type, Integer capacity, BigDecimal price, Boolean available) {
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.price = price;
        this.available = available;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
}