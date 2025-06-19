package com.example.price_comparator.service;

import com.example.price_comparator.dto.ShoppingProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GoogleShoppingStrategyTest {

    private static final Logger logger = LoggerFactory.getLogger(GoogleShoppingStrategyTest.class);

    public static void main(String[] args) {
        OxylabsShoppingScraper scraper = new OxylabsShoppingScraper();
        
        // Get credentials from environment variables
        String username = System.getenv("OXYLABS_USERNAME");
        String password = System.getenv("OXYLABS_PASSWORD");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            logger.error("Oxylabs credentials are not set. Please set the OXYLABS_USERNAME and OXYLABS_PASSWORD environment variables.");
            return;
        }

        // Test query that should find retailer URLs requiring Google Shopping URL lookup
        String query = "iPhone 13 128GB";
        String geoLocation = "United Arab Emirates";
        
        logger.info("=====================================");
        logger.info("ENHANCED GOOGLE SHOPPING STRATEGY TEST");
        logger.info("=====================================");
        logger.info("Testing new strategy:");
        logger.info("1. Find retailer products (Amazon, Noon, Cartlow, etc.)");
        logger.info("2. Find associated Google Shopping URLs for each retailer");
        logger.info("3. Fetch detailed specs from Google Shopping pages");
        logger.info("4. Enhance product matching with detailed specifications");
        logger.info("=====================================");
        logger.info("Query: '{}' in geo-location: '{}'", query, geoLocation);
        
        try {
            // Test the enhanced scraping method
            List<ShoppingProduct> products = scraper.scrapeShoppingResultsEnhanced(query, geoLocation, username, password);

            if (products == null) {
                logger.error("Enhanced scraper returned null");
                return;
            }
            
            if (products.isEmpty()) {
                logger.warn("Enhanced scraper returned empty list");
                return;
            }

            logger.info("=====================================");
            logger.info("ENHANCED SCRAPER RESULTS");
            logger.info("=====================================");
            logger.info("Total products found: {}", products.size());
            
            int enhancedCount = 0;
            int specCount = 0;
            
            for (int i = 0; i < Math.min(10, products.size()); i++) {
                ShoppingProduct product = products.get(i);
                
                logger.info("----------------------------------------------------");
                logger.info("Product {}: {}", i + 1, 
                    product.getTitle().length() > 60 ? product.getTitle().substring(0, 57) + "..." : product.getTitle());
                logger.info("Price: {:.2f} AED", product.getPrice());
                logger.info("Seller: {}", product.getSeller());
                logger.info("URL: {}", product.getProductLink());
                
                if (product.getSpecifications() != null && !product.getSpecifications().isEmpty()) {
                    specCount += product.getSpecifications().size();
                    logger.info("Specifications: {} found", product.getSpecifications().size());
                    
                    // Show first few specs as examples
                    for (int j = 0; j < Math.min(3, product.getSpecifications().size()); j++) {
                        var spec = product.getSpecifications().get(j);
                        logger.info("  - {}: {}", spec.getName(), spec.getValue());
                    }
                    if (product.getSpecifications().size() > 3) {
                        logger.info("  ... and {} more specifications", product.getSpecifications().size() - 3);
                    }
                    enhancedCount++;
                } else {
                    logger.info("Specifications: None extracted");
                }
                
                // Validate URL
                if (product.getProductLink() != null && product.getProductLink().contains("google.com")) {
                    logger.warn("⚠️  Product still has Google domain link: {}", product.getProductLink());
                }
            }
            
            logger.info("=====================================");
            logger.info("ENHANCEMENT SUMMARY");
            logger.info("=====================================");
            logger.info("Products with specifications: {}/{}", enhancedCount, Math.min(10, products.size()));
            logger.info("Total specifications extracted: {}", specCount);
            logger.info("Average specs per enhanced product: {:.1f}", 
                enhancedCount > 0 ? (double) specCount / enhancedCount : 0);
                
            if (enhancedCount >= 5) {
                logger.info("✅ EXCELLENT: Enhanced strategy working well!");
            } else if (enhancedCount >= 2) {
                logger.info("✅ GOOD: Enhanced strategy partially working");
            } else {
                logger.info("⚠️  NEEDS IMPROVEMENT: Limited enhancement success");
            }
            
        } catch (Exception e) {
            logger.error("Enhanced scraper test failed: {}", e.getMessage(), e);
        }
        
        logger.info("Enhanced scraper test completed.");
    }
}
