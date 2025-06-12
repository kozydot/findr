package com.example.price_comparator;

import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.service.AliexpressApiService;
import com.example.price_comparator.service.AmazonApiService;
import com.example.price_comparator.service.LazadaApiService;
import com.example.price_comparator.service.PriceApiService;
import com.example.price_comparator.service.ShopeeApiService;

public class ScraperTestRunner {

    public static void main(String[] args) {
        // Directly instantiate services without Spring context
        AmazonApiService amazonApiService = new AmazonApiService();
        ShopeeApiService shopeeApiService = new ShopeeApiService();
        LazadaApiService lazadaApiService = new LazadaApiService();
        AliexpressApiService aliexpressApiService = new AliexpressApiService();
        PriceApiService priceApiService = new PriceApiService(amazonApiService, shopeeApiService, lazadaApiService, aliexpressApiService);

        String query = "iphone";

        System.out.println("--- Testing Price Comparison API ---");
        System.out.println("Query: " + query);
        try {
            ProductDocument product = priceApiService.fetchProductData("amazon", query);
            if (product != null) {
                System.out.println("Fetched Product Data from Amazon:");
                System.out.println(product.toString());
            } else {
                System.out.println("Failed to fetch product data from the Amazon API.");
            }

            product = priceApiService.fetchProductData("shopee", query);
            if (product != null) {
                System.out.println("Fetched Product Data from Shopee:");
                System.out.println(product.toString());
            } else {
                System.out.println("Failed to fetch product data from the Shopee API.");
            }

            product = priceApiService.fetchProductData("lazada", query);
            if (product != null) {
                System.out.println("Fetched Product Data from Lazada:");
                System.out.println(product.toString());
            } else {
                System.out.println("Failed to fetch product data from the Lazada API.");
            }

            product = priceApiService.fetchProductData("aliexpress", query);
            if (product != null) {
                System.out.println("Fetched Product Data from Aliexpress:");
                System.out.println(product.toString());
            } else {
                System.out.println("Failed to fetch product data from the Aliexpress API.");
            }
        } catch (Exception e) {
            System.err.println("Error during API test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
