package com.example.price_comparator.model;

public class AliexpressProduct {

    private String productTitle;
    private String productDetailUrl;
    private String mainImageUrl;
    private double salePrice;
    private String shopName;
    private int latestSaleVolume;

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

    public String getMainImageUrl() {
        return mainImageUrl;
    }

    public void setMainImageUrl(String mainImageUrl) {
        this.mainImageUrl = mainImageUrl;
    }

    public double getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(double salePrice) {
        this.salePrice = salePrice;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public int getLatestSaleVolume() {
        return latestSaleVolume;
    }

    public void setLatestSaleVolume(int latestSaleVolume) {
        this.latestSaleVolume = latestSaleVolume;
    }
}