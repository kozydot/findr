package com.example.price_comparator;

import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.service.AliexpressApiService;
import com.example.price_comparator.service.AmazonApiService;
import com.example.price_comparator.service.PriceApiService;

public class ScraperTestRunner {

    public static void main(String[] args) {
        // Directly instantiate services without Spring context
        AmazonApiService amazonApiService = new AmazonApiService();
        AliexpressApiService aliexpressApiService = new AliexpressApiService();
        PriceApiService priceApiService = new PriceApiService(amazonApiService, aliexpressApiService);

        String query = "iphone";

        System.out.println("--- Testing Price Comparison API ---");
        System.out.println("Query: " + query);
        try {
            java.util.List<ProductDocument> products = priceApiService.fetchProductData("amazon", query);
            if (products != null && !products.isEmpty()) {
                System.out.println("Fetched Product Data from Amazon:");
                for (ProductDocument product : products) {
                    System.out.println(product.toString());
                }
            } else {
                System.out.println("Failed to fetch product data from the Amazon API.");
            }

            products = priceApiService.fetchProductData("aliexpress", query);
            if (products != null && !products.isEmpty()) {
                System.out.println("Fetched Product Data from Aliexpress:");
                for (ProductDocument product : products) {
                    System.out.println(product.toString());
                }
            } else {
                System.out.println("Failed to fetch product data from the Aliexpress API.");
            }
        } catch (Exception e) {
            System.err.println("Error during API test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
