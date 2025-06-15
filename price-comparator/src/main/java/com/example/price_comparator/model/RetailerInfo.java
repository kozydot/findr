package com.example.price_comparator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetailerInfo {

    private String retailerId; // e.g., "noon", "luluhypermarket"
    private String name;
    private String logo;
    private double currentPrice;
    private List<PriceHistoryPoint> priceHistory = new ArrayList<>();
    private boolean inStock;
    private boolean freeShipping;
    private double shippingCost;
    private String productUrl;
}
