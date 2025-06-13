package com.example.price_comparator.dto;

public class AliexpressMatch {

    private String productTitle;
    private String productDetailUrl;
    private double salePriceUSD;
    private String shopName;

    public AliexpressMatch(String productTitle, String productDetailUrl, double salePriceUSD, String shopName) {
        this.productTitle = productTitle;
        this.productDetailUrl = productDetailUrl;
        this.salePriceUSD = salePriceUSD;
        this.shopName = shopName;
    }

    // Getters and Setters
    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public String getProductDetailUrl() {
        return productDetailUrl;
    }

    public void setProductDetailUrl(String productDetailUrl) {
        this.productDetailUrl = productDetailUrl;
    }

    public double getSalePriceUSD() {
        return salePriceUSD;
    }

    public void setSalePriceUSD(double salePriceUSD) {
        this.salePriceUSD = salePriceUSD;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }
}