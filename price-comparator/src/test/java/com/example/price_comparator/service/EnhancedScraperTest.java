package com.example.price_comparator.service;

import com.example.price_comparator.model.SpecificationInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Enhanced Shopping Scraper Tests")
public class EnhancedScraperTest {

    private OxylabsShoppingScraper scraper;

    @BeforeEach
    void setUp() {
        scraper = new OxylabsShoppingScraper();
    }

    @Test
    @DisplayName("Test Enhanced Image URL Extraction")
    void testEnhancedImageExtraction() throws Exception {
        System.out.println("\n=== Testing Enhanced Image URL Extraction ===");
        
        // Test Case 1: Simple thumbnail field
        JSONObject item1 = new JSONObject();
        item1.put("thumbnail", "https://example.com/image1.jpg");
        
        String imageUrl1 = invokePrivateMethod("extractBestImageUrl", item1);
        assertEquals("https://example.com/image1.jpg", imageUrl1);
        System.out.println("✓ Test 1 - Simple thumbnail: " + imageUrl1);
        
        // Test Case 2: Image object with URL
        JSONObject item2 = new JSONObject();
        JSONObject imageObj = new JSONObject();
        imageObj.put("url", "https://example.com/high-res-image.jpg");
        imageObj.put("width", 800);
        imageObj.put("height", 600);
        item2.put("image", imageObj);
        
        String imageUrl2 = invokePrivateMethod("extractBestImageUrl", item2);
        assertEquals("https://example.com/high-res-image.jpg", imageUrl2);
        System.out.println("✓ Test 2 - Image object: " + imageUrl2);
        
        // Test Case 3: Images array with different sizes
        JSONObject item3 = new JSONObject();
        JSONArray images = new JSONArray();
        
        JSONObject img1 = new JSONObject();
        img1.put("url", "https://example.com/small.jpg");
        img1.put("width", 100);
        img1.put("height", 100);
        images.put(img1);
        
        JSONObject img2 = new JSONObject();
        img2.put("url", "https://example.com/large.jpg");
        img2.put("width", 1000);
        img2.put("height", 800);
        images.put(img2);
        
        item3.put("images", images);
        
        String imageUrl3 = invokePrivateMethod("extractBestImageUrl", item3);
        assertEquals("https://example.com/large.jpg", imageUrl3);
        System.out.println("✓ Test 3 - Best from array: " + imageUrl3);
        
        // Test Case 4: No image available
        JSONObject item4 = new JSONObject();
        item4.put("title", "Product without image");
        
        String imageUrl4 = invokePrivateMethod("extractBestImageUrl", item4);
        assertNull(imageUrl4);
        System.out.println("✓ Test 4 - No image: " + imageUrl4);
    }

    @Test
    @DisplayName("Test Enhanced Specifications Extraction")
    void testEnhancedSpecificationsExtraction() throws Exception {
        System.out.println("\n=== Testing Enhanced Specifications Extraction ===");
        
        // Test Case 1: Direct specifications array
        JSONObject item1 = new JSONObject();
        JSONArray specs = new JSONArray();
        
        JSONObject spec1 = new JSONObject();
        spec1.put("name", "Brand");
        spec1.put("value", "OnePlus");
        specs.put(spec1);
        
        JSONObject spec2 = new JSONObject();
        spec2.put("name", "Color");
        spec2.put("value", "Soft Jade");
        specs.put(spec2);
        
        item1.put("specifications", specs);
        item1.put("title", "OnePlus Nord Buds 3 Pro");
        
        @SuppressWarnings("unchecked")
        List<SpecificationInfo> extracted1 = (List<SpecificationInfo>) invokePrivateMethod("extractEnhancedSpecifications", item1, "OnePlus Nord Buds 3 Pro");
        
        assertNotNull(extracted1);
        assertTrue(extracted1.size() >= 2);
        System.out.println("✓ Test 1 - Direct specs array: " + extracted1.size() + " specifications");
        extracted1.forEach(spec -> System.out.println("  - " + spec.getName() + ": " + spec.getValue()));
        
        // Test Case 2: About this item text parsing
        JSONObject item2 = new JSONObject();
        item2.put("title", "Gaming Laptop");
        item2.put("about_this_item", 
            "• Processor: Intel Core i7-12700H\n" +
            "• RAM: 16GB DDR4\n" +
            "• Storage: 512GB NVMe SSD\n" +
            "• Graphics: NVIDIA RTX 3060\n" +
            "• Display: 15.6-inch Full HD");
        
        @SuppressWarnings("unchecked")
        List<SpecificationInfo> extracted2 = (List<SpecificationInfo>) invokePrivateMethod("extractEnhancedSpecifications", item2, "Gaming Laptop");
        
        assertNotNull(extracted2);
        assertTrue(extracted2.size() >= 5);
        System.out.println("✓ Test 2 - About this item parsing: " + extracted2.size() + " specifications");
        extracted2.forEach(spec -> System.out.println("  - " + spec.getName() + ": " + spec.getValue()));
        
        // Test Case 3: Product details object
        JSONObject item3 = new JSONObject();
        item3.put("title", "Bluetooth Earbuds");
        
        JSONObject details = new JSONObject();
        details.put("battery_life", "44 hours");
        details.put("noise_cancellation", "Active");
        details.put("water_resistance", "IPX4");
        details.put("driver_size", "12.4mm");
        
        item3.put("details", details);
        
        @SuppressWarnings("unchecked")
        List<SpecificationInfo> extracted3 = (List<SpecificationInfo>) invokePrivateMethod("extractEnhancedSpecifications", item3, "Bluetooth Earbuds");
        
        assertNotNull(extracted3);
        assertTrue(extracted3.size() >= 4);
        System.out.println("✓ Test 3 - Details object: " + extracted3.size() + " specifications");
        extracted3.forEach(spec -> System.out.println("  - " + spec.getName() + ": " + spec.getValue()));
        
        // Test Case 4: Mixed sources
        JSONObject item4 = new JSONObject();
        item4.put("title", "Complex Product");
        item4.put("brand", "Apple");
        item4.put("color", "Space Gray");
        item4.put("description", "Model: iPhone 15 Pro\nStorage: 256GB\nCamera: 48MP");
        
        JSONObject attributes = new JSONObject();
        attributes.put("warranty", "1 year");
        attributes.put("country_of_origin", "China");
        item4.put("attributes", attributes);
        
        @SuppressWarnings("unchecked")
        List<SpecificationInfo> extracted4 = (List<SpecificationInfo>) invokePrivateMethod("extractEnhancedSpecifications", item4, "Complex Product");
        
        assertNotNull(extracted4);
        System.out.println("✓ Test 4 - Mixed sources: " + extracted4.size() + " specifications");
        extracted4.forEach(spec -> System.out.println("  - " + spec.getName() + ": " + spec.getValue()));
    }

    @Test
    @DisplayName("Test Text Specification Parsing")
    void testTextSpecificationParsing() throws Exception {
        System.out.println("\n=== Testing Text Specification Parsing ===");
        
        // Test different text formats
        String[] testTexts = {
            "Brand: OnePlus\nColor: Soft Jade\nBattery: 44 hours\nNoise Cancellation: Active",
            "• Processor: Intel i7\n• Memory: 16GB RAM\n• Storage: 512GB SSD",
            "Model - iPhone 15\nStorage - 256GB\nCamera - 48MP\nDisplay - 6.1 inch",
            "Weight: 1.2kg; Dimensions: 35x24x2cm; Material: Aluminum"
        };
        
        for (int i = 0; i < testTexts.length; i++) {
            @SuppressWarnings("unchecked")
            List<SpecificationInfo> parsed = (List<SpecificationInfo>) invokePrivateMethod("parseSpecificationsFromText", testTexts[i], "test" + i);
            
            System.out.println("✓ Test format " + (i + 1) + ": " + parsed.size() + " specifications");
            parsed.forEach(spec -> System.out.println("  - " + spec.getName() + ": " + spec.getValue()));
            System.out.println();
        }
    }

    @Test
    @DisplayName("Test Specification Deduplication")
    void testSpecificationDeduplication() throws Exception {
        System.out.println("\n=== Testing Specification Deduplication ===");
        
        // Create test specifications with duplicates
        JSONObject item = new JSONObject();
        item.put("title", "Test Product");
        item.put("brand", "TestBrand");
        item.put("color", "Red");
        
        JSONArray specs = new JSONArray();
        JSONObject spec1 = new JSONObject();
        spec1.put("name", "Brand");
        spec1.put("value", "TestBrand (Official)");
        specs.put(spec1);
        
        JSONObject spec2 = new JSONObject();
        spec2.put("name", "Color");
        spec2.put("value", "Red Color");
        specs.put(spec2);
        
        item.put("specifications", specs);
        
        @SuppressWarnings("unchecked")
        List<SpecificationInfo> result = (List<SpecificationInfo>) invokePrivateMethod("extractEnhancedSpecifications", item, "Test Product");
        
        System.out.println("✓ Deduplication test: " + result.size() + " unique specifications");
        result.forEach(spec -> System.out.println("  - " + spec.getName() + ": " + spec.getValue()));
        
        // Verify no exact duplicates by name
        long uniqueNames = result.stream().map(SpecificationInfo::getName).distinct().count();
        assertEquals(uniqueNames, result.size(), "Should have no duplicate specification names");
    }

    @Test
    @DisplayName("Test Real Product Data Structure")
    void testRealProductDataStructure() throws Exception {
        System.out.println("\n=== Testing Real Product Data Structure ===");
        
        // Simulate a realistic Google Shopping response structure
        JSONObject realProduct = new JSONObject();
        realProduct.put("title", "OnePlus Nord Buds 3 Pro Truly Wireless Bluetooth Earbuds");
        realProduct.put("url", "https://www.amazon.ae/dp/B0D7HV9SPJ");
        realProduct.put("price", 187.50);
        realProduct.put("thumbnail", "https://m.media-amazon.com/images/I/61abc123def.jpg");
        
        JSONObject merchant = new JSONObject();
        merchant.put("name", "Amazon.ae");
        merchant.put("url", "https://www.amazon.ae");
        realProduct.put("merchant", merchant);
        
        // Add realistic product content
        JSONObject content = new JSONObject();
        content.put("product_details", 
            "• Up to 49dB Active Noise Cancellation\n" +
            "• 12.4mm Dynamic Drivers\n" +
            "• 10mins for 11Hrs Fast charging\n" +
            "• Up to 44Hrs Music Playback\n" +
            "• Hi-Res Audio certified\n" +
            "• IP55 Water & Dust Resistance");
        
        JSONArray contentSpecs = new JSONArray();
        JSONObject spec1 = new JSONObject();
        spec1.put("name", "Battery Life");
        spec1.put("value", "44 hours total");
        contentSpecs.put(spec1);
        
        JSONObject spec2 = new JSONObject();
        spec2.put("name", "Driver Size");
        spec2.put("value", "12.4mm");
        contentSpecs.put(spec2);
        
        content.put("specifications", contentSpecs);
        realProduct.put("content", content);
        
        // Test extraction
        String imageUrl = invokePrivateMethod("extractBestImageUrl", realProduct);
        @SuppressWarnings("unchecked")
        List<SpecificationInfo> specs = (List<SpecificationInfo>) invokePrivateMethod("extractEnhancedSpecifications", realProduct, realProduct.getString("title"));
        
        System.out.println("✓ Real product test:");
        System.out.println("  - Title: " + realProduct.getString("title"));
        System.out.println("  - Image URL: " + imageUrl);
        System.out.println("  - Specifications found: " + specs.size());
        
        specs.forEach(spec -> System.out.println("    • " + spec.getName() + ": " + spec.getValue()));
        
        assertNotNull(imageUrl);
        assertTrue(specs.size() >= 6, "Should extract at least 6 specifications");
        
        // Verify specific specifications were extracted
        assertTrue(specs.stream().anyMatch(s -> s.getName().toLowerCase().contains("battery")), "Should have battery specification");
        assertTrue(specs.stream().anyMatch(s -> s.getName().toLowerCase().contains("driver")), "Should have driver specification");
    }

    @Test
    @DisplayName("Integration Test with Mock Oxylabs Response")
    void testFullIntegrationWithMockResponse() {
        System.out.println("\n=== Integration Test with Mock Response ===");
        
        // This test would require mocking the actual HTTP response
        // For now, we'll just verify the service can be instantiated
        assertNotNull(scraper);
        System.out.println("✓ Scraper service instantiated successfully");
        
        // TODO: Add full integration test with mocked Oxylabs API response
        // This would test the complete flow: API call -> JSON parsing -> product extraction
    }

    // Helper method to invoke private methods for testing
    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(String methodName, Object... args) throws Exception {
        Class<?>[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
            // Handle primitive type mapping
            if (paramTypes[i] == String.class && methodName.equals("extractEnhancedSpecifications") && i == 1) {
                // Keep String.class for the second parameter of extractEnhancedSpecifications
            }
        }
        
        Method method = OxylabsShoppingScraper.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return (T) method.invoke(scraper, args);
    }

    @Test
    @DisplayName("Performance Test - Large Data Extraction")
    void testPerformanceWithLargeData() throws Exception {
        System.out.println("\n=== Performance Test ===");
        
        long startTime = System.currentTimeMillis();
        
        // Create a large product with many specifications
        JSONObject largeProduct = new JSONObject();
        largeProduct.put("title", "Complex Electronics Product with Many Specifications");
        
        // Add many specifications
        JSONArray specs = new JSONArray();
        for (int i = 0; i < 50; i++) {
            JSONObject spec = new JSONObject();
            spec.put("name", "Specification " + i);
            spec.put("value", "Value " + i + " with detailed description");
            specs.put(spec);
        }
        largeProduct.put("specifications", specs);
        
        // Add large text content
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeText.append("• Feature ").append(i).append(": Detailed description of feature ").append(i).append("\n");
        }
        largeProduct.put("about_this_item", largeText.toString());
        
        @SuppressWarnings("unchecked")
        List<SpecificationInfo> result = (List<SpecificationInfo>) invokePrivateMethod("extractEnhancedSpecifications", largeProduct, "Complex Product");
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("✓ Performance test completed:");
        System.out.println("  - Processing time: " + duration + "ms");
        System.out.println("  - Specifications extracted: " + result.size());
        System.out.println("  - Performance: " + (result.size() / (duration / 1000.0)) + " specs/second");
        
        assertTrue(duration < 5000, "Should complete within 5 seconds");
        assertTrue(result.size() > 100, "Should extract many specifications");
    }
}
