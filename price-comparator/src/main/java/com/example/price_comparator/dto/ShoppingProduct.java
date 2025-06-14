package com.example.price_comparator.dto;

import lombok.Data;

@Data
public class ShoppingProduct {
    private String title;
    private double price;
    private String seller;
    private String productLink;
    private String imageUrl;
}