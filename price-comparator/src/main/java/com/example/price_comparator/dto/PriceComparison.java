package com.example.price_comparator.dto;

public class PriceComparison {

    private double amazonPriceAed;
    private double aliexpressPriceAed;
    private double priceDifferenceAed;
    private String cheaperStore;
    private String notes;

    public PriceComparison(double amazonPriceAed, double aliexpressPriceAed, double priceDifferenceAed, String cheaperStore, String notes) {
        this.amazonPriceAed = amazonPriceAed;
        this.aliexpressPriceAed = aliexpressPriceAed;
        this.priceDifferenceAed = priceDifferenceAed;
        this.cheaperStore = cheaperStore;
        this.notes = notes;
    }

    // Getters and Setters
    public double getAmazonPriceAed() {
        return amazonPriceAed;
    }

    public void setAmazonPriceAed(double amazonPriceAed) {
        this.amazonPriceAed = amazonPriceAed;
    }

    public double getAliexpressPriceAed() {
        return aliexpressPriceAed;
    }

    public void setAliexpressPriceAed(double aliexpressPriceAed) {
        this.aliexpressPriceAed = aliexpressPriceAed;
    }

    public double getPriceDifferenceAed() {
        return priceDifferenceAed;
    }

    public void setPriceDifferenceAed(double priceDifferenceAed) {
        this.priceDifferenceAed = priceDifferenceAed;
    }

    public String getCheaperStore() {
        return cheaperStore;
    }

    public void setCheaperStore(String cheaperStore) {
        this.cheaperStore = cheaperStore;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}