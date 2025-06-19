package com.example.price_comparator.service;

import com.example.price_comparator.dto.ShoppingProduct;
import com.example.price_comparator.model.SpecificationInfo;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify the enhanced scraper handles different data types correctly
 * especially for numeric values like ratings that caused JSONException
 */
@DisplayName("Enhanced Scraper Error Handling Tests")
public class OxylabsScraperErrorHandlingTest {
    
    private static final Logger logger = LoggerFactory.getLogger(OxylabsScraperErrorHandlingTest.class);
    private OxylabsShoppingScraper scraper;
    
    @BeforeEach
    void setUp() {
        scraper = new OxylabsShoppingScraper();
    }
    
    @Test
    @DisplayName("Should handle numeric rating values without throwing JSONException")
    void testNumericRatingHandling() throws Exception {
        // Create test JSON with numeric rating (the problematic case)
        JSONObject testProduct = new JSONObject();
        testProduct.put("title", "Test Product");
        testProduct.put("url", "https://test.com/product");
        testProduct.put("price", "99.99");
        testProduct.put("rating", 4.4); // This is a BigDecimal/Number, not String
        testProduct.put("reviews_count", 1250); // Another numeric field
        testProduct.put("brand", "TestBrand");
        
        logger.info("Testing product with numeric rating: {}", testProduct.toString(2));
        
        // Use reflection to call the private extractCommonProductAttributes method
        Method extractMethod = OxylabsShoppingScraper.class.getDeclaredMethod(
            "extractCommonProductAttributes", JSONObject.class, List.class);
        extractMethod.setAccessible(true);
        
        List<SpecificationInfo> specifications = new ArrayList<>();
        
        // This should not throw an exception anymore
        assertDoesNotThrow(() -> {
            try {
                extractMethod.invoke(scraper, testProduct, specifications);
                logger.info("Successfully extracted attributes without exception");
            } catch (Exception e) {
                logger.error("Extraction failed: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
        
        // Verify specifications were extracted
        assertFalse(specifications.isEmpty(), "Should extract at least some specifications");
        
        // Check that rating was extracted as string
        boolean ratingFound = specifications.stream()
            .anyMatch(spec -> "Rating".equals(spec.getName()) && "4.4".equals(spec.getValue()));
        assertTrue(ratingFound, "Rating should be extracted as '4.4'");
        
        // Check that reviews count was extracted
        boolean reviewsFound = specifications.stream()
            .anyMatch(spec -> "Reviews Count".equals(spec.getName()) && "1250".equals(spec.getValue()));
        assertTrue(reviewsFound, "Reviews count should be extracted as '1250'");
        
        logger.info("Test passed: Found {} specifications", specifications.size());
        specifications.forEach(spec -> 
            logger.info("  - {}: {}", spec.getName(), spec.getValue()));
    }
    
    @Test
    @DisplayName("Should handle enhanced specification extraction with error recovery")
    void testEnhancedSpecificationExtractionErrorRecovery() throws Exception {
        // Create test JSON that might cause issues
        JSONObject testProduct = new JSONObject();
        testProduct.put("title", "JBL Tune 520BT Headphones");
        testProduct.put("url", "https://test.com/jbl");
        testProduct.put("price", "110.00");
        testProduct.put("rating", 4.4); // Numeric rating
        testProduct.put("reviews_count", 856);
        testProduct.put("brand", "JBL");
        testProduct.put("color", "Blue");
        testProduct.put("description", "Wireless headphones with pure bass sound");
        
        // Add some problematic fields that might cause issues
        testProduct.put("specifications", new org.json.JSONArray()); // Empty array
        testProduct.put("about_this_item", "57H Battery Life • Pure Bass Sound • Multi-Point Connection");        
        logger.info("Testing enhanced specification extraction with potential error conditions");
        
        // Use reflection to call the private extractEnhancedSpecifications method
        Method extractMethod = OxylabsShoppingScraper.class.getDeclaredMethod(
            "extractEnhancedSpecifications", JSONObject.class, String.class);
        extractMethod.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        final List<SpecificationInfo>[] specifications = (List<SpecificationInfo>[]) new List[1];
        
        // This should not throw an exception and should return a valid list
        assertDoesNotThrow(() -> {
            try {
                Object result = extractMethod.invoke(scraper, testProduct, testProduct.getString("title"));
                assertTrue(result instanceof List, "Should return a List of specifications");
                @SuppressWarnings("unchecked")
                List<SpecificationInfo> specs = (List<SpecificationInfo>) result;
                logger.info("Successfully extracted {} specifications", specs.size());
                
                // Set for further verification
                specifications[0] = specs;
            } catch (Exception e) {
                logger.error("Enhanced extraction failed: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
        
        assertNotNull(specifications[0], "Specifications list should not be null");
        
        // Should have extracted some specifications from various sources
        assertTrue(specifications[0].size() > 0, "Should extract at least some specifications");
        
        logger.info("Enhanced extraction test passed: Found {} specifications", specifications[0].size());
        specifications[0].forEach(spec -> 
            logger.info("  - {}: {}", spec.getName(), spec.getValue()));
    }
    
    @Test
    @DisplayName("Should create valid ShoppingProduct from problematic JSON without errors")
    void testCompleteProductCreationErrorRecovery() {
        // Simulate the exact JSON structure that caused the original error
        JSONObject testItem = new JSONObject();
        testItem.put("title", "JBL Tune 520BT Wireless Headphones");
        testItem.put("url", "https://example.com/jbl-headphones");
        testItem.put("price", "96.43");
        testItem.put("rating", 4.4); // This caused the original JSONException
        testItem.put("reviews_count", 1250);
        testItem.put("seller", "Microless.com");
        testItem.put("thumbnail", "https://example.com/image.jpg");
        testItem.put("description", "Pure Bass Sound, 57H Battery with Speed Charge");
        
        org.json.JSONArray products = new org.json.JSONArray();
        products.put(testItem);
        
        logger.info("Testing complete product creation with problematic JSON structure");
        
        // Use reflection to call the private extractProducts method
        assertDoesNotThrow(() -> {
            try {
                Method extractMethod = OxylabsShoppingScraper.class.getDeclaredMethod(
                    "extractProducts", org.json.JSONArray.class, List.class);
                extractMethod.setAccessible(true);
                
                List<ShoppingProduct> productList = new ArrayList<>();
                extractMethod.invoke(scraper, products, productList);
                
                assertFalse(productList.isEmpty(), "Should create at least one product");
                
                ShoppingProduct product = productList.get(0);
                assertNotNull(product.getTitle(), "Product should have a title");
                assertTrue(product.getPrice() > 0, "Product should have a valid price");
                assertNotNull(product.getSeller(), "Product should have a seller");
                
                // Check that specifications were extracted without errors
                if (product.getSpecifications() != null) {
                    logger.info("Product has {} specifications", product.getSpecifications().size());
                    product.getSpecifications().forEach(spec -> 
                        logger.info("  - {}: {}", spec.getName(), spec.getValue()));
                }
                
                logger.info("Complete product creation test passed successfully");
                
            } catch (Exception e) {
                logger.error("Product creation failed: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }
}
