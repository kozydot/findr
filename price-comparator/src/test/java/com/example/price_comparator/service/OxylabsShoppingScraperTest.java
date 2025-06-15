package com.example.price_comparator.service;

import com.example.price_comparator.dto.ShoppingProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

public class OxylabsShoppingScraperTest {

    private static final Logger logger = LoggerFactory.getLogger(OxylabsShoppingScraperTest.class);

    public static void main(String[] args) {
        OxylabsShoppingScraper scraper = new OxylabsShoppingScraper();
        
        // IMPORTANT: Replace with your actual Oxylabs credentials
        String username = System.getenv("OXYLABS_USERNAME");
        String password = System.getenv("OXYLABS_PASSWORD");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            logger.error("Oxylabs credentials are not set. Please set the OXYLABS_USERNAME and OXYLABS_PASSWORD environment variables.");
            return;
        }

        ReflectionTestUtils.setField(scraper, "username", username);
        ReflectionTestUtils.setField(scraper, "password", password);

        String query = "GENZY Smart Bakhoor Burner, Portable";
        String geoLocation = "United Arab Emirates";
        
        logger.info("Starting test for query: '{}' in geo-location: '{}'", query, geoLocation);
        
        List<ShoppingProduct> products = scraper.scrapeShoppingResults(query, geoLocation, username, password);

        if (products == null) {
            logger.error("The product list is null. The test cannot proceed.");
            return;
        }
        
        if (products.isEmpty()) {
            logger.warn("The product list is empty. No products found for the query.");
        } else {
            logger.info("Found {} products for query: '{}'", products.size(), query);
        }

        for (ShoppingProduct product : products) {
            logger.info("----------------------------------------------------");
            logger.info("Title: {}", product.getTitle());
            logger.info("Price: {}", product.getPrice());
            logger.info("Seller: {}", product.getSeller());
            logger.info("Product Link: {}", product.getProductLink());
            logger.info("Image URL: {}", product.getImageUrl());
            logger.info("----------------------------------------------------");
            
            if (product.getTitle() == null) {
                logger.error("Product title is null.");
            }
            if (product.getProductLink() == null) {
                logger.error("Product link is null.");
            } else {
                if (product.getProductLink().isEmpty()) {
                    logger.error("Product link is empty.");
                }
                if (product.getProductLink().contains("google.com")) {
                    logger.error("Product link is a Google domain link: {}", product.getProductLink());
                }
            }
        }
        
        logger.info("Test finished for query: '{}'", query);
    }
}
