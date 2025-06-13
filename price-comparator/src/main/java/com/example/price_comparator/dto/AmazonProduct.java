package com.example.price_comparator.dto;

public class AmazonProduct {

    private String title;
    private double priceAed;

    public AmazonProduct(String title, double priceAed) {
        this.title = title;
        this.priceAed = priceAed;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getPriceAed() {
        return priceAed;
    }

    public void setPriceAed(double priceAed) {
        this.priceAed = priceAed;
    }
}