package com.example.price_comparator;

import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.service.AmazonApiService;
import com.example.price_comparator.service.PriceApiService;

public class ScraperTestRunner {

    public static void main(String[] args) {
        // Directly instantiate services without Spring context
        AmazonApiService amazonApiService = new AmazonApiService();
        PriceApiService priceApiService = new PriceApiService(amazonApiService);

        String query = "iphone";

        System.out.println("--- Testing Price Comparison API ---");
        System.out.println("Query: " + query);
        try {
            ProductDocument product = priceApiService.fetchProductData("amazon", query);
            if (product != null) {
                System.out.println("Fetched Product Data:");
                System.out.println(product.toString());
            } else {
                System.out.println("Failed to fetch product data from the API.");
            }
        } catch (Exception e) {
            System.err.println("Error during API test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
