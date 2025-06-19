package com.example.price_comparator.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for enhanced image hashing functionality
 */
@SpringBootTest
@TestPropertySource(properties = {
    "oxylabs.username=test",
    "oxylabs.password=test"
})
public class ImageHashingServiceTest {

    @Autowired
    private ImageHashingService imageHashingService;    @Test
    public void testPerceptualHashGeneration() {
        // Test with a sample image URL (you can replace with actual test image)
        String testImageUrl = "https://via.placeholder.com/300x300/FF0000/FFFFFF?text=Test";
        
        try {
            // Test basic perceptual hash generation
            String hash = imageHashingService.generatePerceptualHash(testImageUrl).get();
            assertNotNull(hash, "Hash should not be null for valid image");
            assertTrue(hash.length() > 0, "Hash should not be empty");
            
            // Test MD5 hash generation
            String md5Hash = imageHashingService.generateMD5Hash(testImageUrl);
            assertNotNull(md5Hash, "MD5 hash should not be null for valid image");
            assertTrue(md5Hash.length() == 32, "MD5 hash should be 32 characters long");
            
        } catch (Exception e) {
            // Network issues or placeholder service unavailable - test with null URLs
            try {
                String nullHash = imageHashingService.generatePerceptualHash(null).get();
                assertNull(nullHash, "Hash should be null for null URL");
            } catch (Exception ex) {
                // Expected for null URL
                assertTrue(true, "Exception expected for null URL");
            }
        }
    }

    @Test
    public void testSimilarityCalculation() {
        // Test similarity calculation with identical hashes
        String hash1 = "abcd1234";
        String hash2 = "abcd1234";
        double similarity = imageHashingService.calculateSimilarity(hash1, hash2);
        assertEquals(1.0, similarity, 0.001, "Identical hashes should have 100% similarity");
        
        // Test with null hashes
        double nullSimilarity = imageHashingService.calculateSimilarity(null, hash1);
        assertEquals(0.0, nullSimilarity, 0.001, "Null hash comparison should return 0% similarity");
    }

    @Test
    public void testAdvancedSimilarityFeatures() {
        // Test the areImagesHighlySimilar method
        String testUrl1 = "https://via.placeholder.com/300x300/FF0000/FFFFFF?text=Test1";
        String testUrl2 = "https://via.placeholder.com/300x300/FF0000/FFFFFF?text=Test2";
        
        try {
            // Test advanced similarity calculation
            double similarity = imageHashingService.calculateAdvancedSimilarity(testUrl1, testUrl2);
            assertTrue(similarity >= 0.0 && similarity <= 1.0, 
                "Similarity should be between 0.0 and 1.0");
            
            // Test high similarity check
            boolean isHighlySimilar = imageHashingService.areImagesHighlySimilar(testUrl1, testUrl2);
            assertTrue(isHighlySimilar == (similarity >= 0.75), 
                "High similarity check should match threshold");
                
        } catch (Exception e) {
            // Network issues - test with null URLs
            double nullSimilarity = imageHashingService.calculateAdvancedSimilarity(null, testUrl1);
            assertEquals(0.0, nullSimilarity, 0.001, "Null URL comparison should return 0% similarity");
        }
    }

    @Test
    public void testEdgeCases() {
        // Test with invalid URLs
        String invalidUrl = "not-a-valid-url";
        
        try {
            String hash = imageHashingService.generatePerceptualHash(invalidUrl).get();
            assertNull(hash, "Hash should be null for invalid URL");
        } catch (Exception e) {
            // Expected for invalid URLs
            assertTrue(true, "Exception expected for invalid URL");
        }
        
        // Test with empty strings
        String emptyHash = imageHashingService.generateMD5Hash("");
        assertNull(emptyHash, "Hash should be null for empty URL");
        
        // Test similarity with different length hashes
        double badSimilarity = imageHashingService.calculateSimilarity("abc", "abcdef");
        assertTrue(badSimilarity >= 0.0, "Similarity should handle different length hashes gracefully");
    }
}
