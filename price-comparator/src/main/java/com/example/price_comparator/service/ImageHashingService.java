package com.example.price_comparator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced service for generating and comparing perceptual hashes of product images
 * to improve product matching accuracy through visual similarity.
 * 
 * Features:
 * - Perceptual hashing (dHash) for layout similarity
 * - Average hash (aHash) for color distribution similarity
 * - Color histogram analysis for color similarity
 * - Edge detection hashing for shape similarity
 * - Multi-algorithm scoring for comprehensive matching
 */
@Service
public class ImageHashingService {

    private static final Logger logger = LoggerFactory.getLogger(ImageHashingService.class);
    
    // Hash dimensions for perceptual hash calculation
    private static final int HASH_SIZE = 8;
    private static final int LARGE_HASH_SIZE = 16; // For high-precision matching
    
    // Connection timeout for image downloads
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    private static final int READ_TIMEOUT = 10000; // 10 seconds
      // Color histogram parameters
    private static final int HISTOGRAM_BINS = 32; // Number of bins per color channel

    /**
     * Check if images meet similarity thresholds for different algorithms
     */
    public boolean areImagesHighlySimilar(String imageUrl1, String imageUrl2) {
        double similarity = calculateAdvancedSimilarity(imageUrl1, imageUrl2);
        return similarity >= 0.75; // Combined threshold for high confidence matching
    }

    /**
     * Generate a perceptual hash for an image URL
     * Returns null if image cannot be processed
     */
    public CompletableFuture<String> generatePerceptualHash(String imageUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    return null;
                }
                
                BufferedImage image = downloadImage(imageUrl);
                if (image == null) {
                    return null;
                }
                
                return calculatePerceptualHash(image);
                
            } catch (Exception e) {
                logger.debug("Failed to generate perceptual hash for image: {} - {}", imageUrl, e.getMessage());
                return null;
            }
        }).orTimeout(15, TimeUnit.SECONDS)
          .exceptionally(throwable -> {
              logger.debug("Timeout or error generating hash for: {} - {}", imageUrl, throwable.getMessage());
              return null;
          });
    }    /**
     * Enhanced image comparison that combines multiple algorithms
     * Returns a comprehensive similarity score between 0.0 and 1.0
     */
    public double calculateAdvancedSimilarity(String imageUrl1, String imageUrl2) {
        try {
            if (imageUrl1 == null || imageUrl2 == null || 
                imageUrl1.trim().isEmpty() || imageUrl2.trim().isEmpty()) {
                logger.debug("Advanced similarity: null/empty URLs provided");
                return 0.0;
            }
            
            // Check for exact URL match first
            if (imageUrl1.equals(imageUrl2)) {
                logger.debug("Advanced similarity: exact URL match");
                return 1.0;
            }
            
            BufferedImage image1 = downloadImage(imageUrl1);
            BufferedImage image2 = downloadImage(imageUrl2);
            
            if (image1 == null) {
                logger.debug("Advanced similarity: failed to download image1 from {}", 
                    imageUrl1.length() > 50 ? imageUrl1.substring(0, 47) + "..." : imageUrl1);
                return 0.0;
            }
            
            if (image2 == null) {
                logger.debug("Advanced similarity: failed to download image2 from {}", 
                    imageUrl2.length() > 50 ? imageUrl2.substring(0, 47) + "..." : imageUrl2);
                return 0.0;
            }
            
            // Calculate multiple similarity metrics
            double dHashSimilarity = calculateDHashSimilarity(image1, image2);
            double aHashSimilarity = calculateAHashSimilarity(image1, image2);
            double colorSimilarity = calculateColorHistogramSimilarity(image1, image2);
            double edgeSimilarity = calculateEdgeSimilarity(image1, image2);
            
            // Weighted combination of all metrics
            double weightedScore = (dHashSimilarity * 0.35) +    // Layout similarity
                                 (aHashSimilarity * 0.25) +     // Overall appearance
                                 (colorSimilarity * 0.25) +     // Color distribution
                                 (edgeSimilarity * 0.15);       // Shape/edge similarity
            
            logger.debug("Advanced similarity analysis - dHash: {}, aHash: {}, color: {}, edge: {}, final: {}", 
                String.format("%.3f", dHashSimilarity),
                String.format("%.3f", aHashSimilarity), 
                String.format("%.3f", colorSimilarity),
                String.format("%.3f", edgeSimilarity),
                String.format("%.3f", weightedScore));
            
            return weightedScore;
            
        } catch (Exception e) {
            logger.debug("Error in advanced similarity calculation: {} - returning 0.0", e.getMessage());
            return 0.0;
        }
    }/**
     * Calculate dHash (difference hash) similarity - good for layout changes
     */
    private double calculateDHashSimilarity(BufferedImage image1, BufferedImage image2) {
        String hash1 = calculatePerceptualHash(image1);
        String hash2 = calculatePerceptualHash(image2);
        return calculateSimilarity(hash1, hash2);
    }
    
    /**
     * Calculate aHash (average hash) similarity - good for color/brightness changes
     */
    private double calculateAHashSimilarity(BufferedImage image1, BufferedImage image2) {
        String hash1 = calculateAverageHash(image1);
        String hash2 = calculateAverageHash(image2);
        return calculateSimilarity(hash1, hash2);
    }
    
    /**
     * Calculate average hash - robust against brightness and contrast changes
     */
    private String calculateAverageHash(BufferedImage image) {
        try {
            // Resize to small thumbnail
            BufferedImage thumbnail = resizeImage(image, HASH_SIZE, HASH_SIZE);
            BufferedImage grayscale = convertToGrayscale(thumbnail);
            
            // Calculate average brightness
            long total = 0;
            for (int y = 0; y < HASH_SIZE; y++) {
                for (int x = 0; x < HASH_SIZE; x++) {
                    total += grayscale.getRGB(x, y) & 0xFF;
                }
            }
            int average = (int) (total / (HASH_SIZE * HASH_SIZE));
            
            // Create hash based on comparison to average
            StringBuilder hash = new StringBuilder();
            for (int y = 0; y < HASH_SIZE; y++) {
                for (int x = 0; x < HASH_SIZE; x++) {
                    int pixel = grayscale.getRGB(x, y) & 0xFF;
                    hash.append(pixel > average ? "1" : "0");
                }
            }
            
            return binaryToHex(hash.toString());
            
        } catch (Exception e) {
            logger.debug("Error calculating average hash: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculate color histogram similarity - good for color distribution matching
     */
    private double calculateColorHistogramSimilarity(BufferedImage image1, BufferedImage image2) {
        try {
            int[] histogram1 = calculateColorHistogram(image1);
            int[] histogram2 = calculateColorHistogram(image2);
            
            return calculateHistogramSimilarity(histogram1, histogram2);
            
        } catch (Exception e) {
            logger.debug("Error calculating color histogram similarity: {}", e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Calculate color histogram for RGB channels
     */
    private int[] calculateColorHistogram(BufferedImage image) {
        // Resize for consistent analysis
        BufferedImage resized = resizeImage(image, 64, 64);
        
        int[] histogram = new int[HISTOGRAM_BINS * 3]; // RGB channels
        
        for (int y = 0; y < resized.getHeight(); y++) {
            for (int x = 0; x < resized.getWidth(); x++) {
                int rgb = resized.getRGB(x, y);
                
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                
                // Map to histogram bins
                int redBin = (red * HISTOGRAM_BINS) / 256;
                int greenBin = (green * HISTOGRAM_BINS) / 256;
                int blueBin = (blue * HISTOGRAM_BINS) / 256;
                
                // Clamp to valid range
                redBin = Math.min(redBin, HISTOGRAM_BINS - 1);
                greenBin = Math.min(greenBin, HISTOGRAM_BINS - 1);
                blueBin = Math.min(blueBin, HISTOGRAM_BINS - 1);
                
                histogram[redBin]++;
                histogram[HISTOGRAM_BINS + greenBin]++;
                histogram[HISTOGRAM_BINS * 2 + blueBin]++;
            }
        }
        
        return histogram;
    }
    
    /**
     * Calculate similarity between two histograms using correlation coefficient
     */
    private double calculateHistogramSimilarity(int[] hist1, int[] hist2) {
        if (hist1.length != hist2.length) {
            return 0.0;
        }
        
        // Calculate means
        double mean1 = Arrays.stream(hist1).average().orElse(0.0);
        double mean2 = Arrays.stream(hist2).average().orElse(0.0);
        
        // Calculate correlation coefficient
        double numerator = 0.0;
        double sumSq1 = 0.0;
        double sumSq2 = 0.0;
        
        for (int i = 0; i < hist1.length; i++) {
            double diff1 = hist1[i] - mean1;
            double diff2 = hist2[i] - mean2;
            
            numerator += diff1 * diff2;
            sumSq1 += diff1 * diff1;
            sumSq2 += diff2 * diff2;
        }
        
        double denominator = Math.sqrt(sumSq1 * sumSq2);
        if (denominator == 0.0) {
            return Arrays.equals(hist1, hist2) ? 1.0 : 0.0;
        }
        
        double correlation = numerator / denominator;
        return Math.max(0.0, correlation); // Return positive correlation only
    }
    
    /**
     * Calculate edge similarity using Sobel edge detection
     */
    private double calculateEdgeSimilarity(BufferedImage image1, BufferedImage image2) {
        try {
            String edgeHash1 = calculateEdgeHash(image1);
            String edgeHash2 = calculateEdgeHash(image2);
            
            return calculateSimilarity(edgeHash1, edgeHash2);
            
        } catch (Exception e) {
            logger.debug("Error calculating edge similarity: {}", e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Calculate edge hash using simplified edge detection
     */
    private String calculateEdgeHash(BufferedImage image) {
        try {
            // Resize and convert to grayscale
            BufferedImage resized = resizeImage(image, HASH_SIZE + 2, HASH_SIZE + 2);
            BufferedImage grayscale = convertToGrayscale(resized);
            
            StringBuilder hash = new StringBuilder();
            
            // Apply simplified Sobel operator
            for (int y = 1; y < HASH_SIZE + 1; y++) {
                for (int x = 1; x < HASH_SIZE + 1; x++) {
                    // Get surrounding pixels
                    int p1 = grayscale.getRGB(x-1, y-1) & 0xFF;
                    int p2 = grayscale.getRGB(x, y-1) & 0xFF;
                    int p3 = grayscale.getRGB(x+1, y-1) & 0xFF;
                    int p4 = grayscale.getRGB(x-1, y) & 0xFF;
                    int p6 = grayscale.getRGB(x+1, y) & 0xFF;
                    int p7 = grayscale.getRGB(x-1, y+1) & 0xFF;
                    int p8 = grayscale.getRGB(x, y+1) & 0xFF;
                    int p9 = grayscale.getRGB(x+1, y+1) & 0xFF;
                    
                    // Sobel X and Y gradients
                    int gx = (p3 + 2*p6 + p9) - (p1 + 2*p4 + p7);
                    int gy = (p1 + 2*p2 + p3) - (p7 + 2*p8 + p9);
                    
                    // Edge magnitude
                    double magnitude = Math.sqrt(gx*gx + gy*gy);
                    
                    // Threshold for edge detection
                    hash.append(magnitude > 50 ? "1" : "0");
                }
            }
            
            return binaryToHex(hash.toString());
            
        } catch (Exception e) {
            logger.debug("Error calculating edge hash: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate a simple MD5 hash for exact image matching
     * Useful for detecting identical images
     */
    public String generateMD5Hash(String imageUrl) {
        try {
            BufferedImage image = downloadImage(imageUrl);
            if (image == null) {
                return null;
            }
            
            // Convert image to byte array for MD5
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(imageBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
            
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.debug("Failed to generate MD5 hash for image: {} - {}", imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Enhanced perceptual hash with high precision option
     */
    public CompletableFuture<String> generateEnhancedPerceptualHash(String imageUrl, boolean highPrecision) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    return null;
                }
                
                BufferedImage image = downloadImage(imageUrl);
                if (image == null) {
                    return null;
                }
                
                int hashSize = highPrecision ? LARGE_HASH_SIZE : HASH_SIZE;
                return calculateDifferenceHashWithSize(image, hashSize);
                
            } catch (Exception e) {
                logger.debug("Failed to generate enhanced perceptual hash: {} - {}", imageUrl, e.getMessage());
                return null;
            }
        }).orTimeout(15, TimeUnit.SECONDS)
          .exceptionally(throwable -> {
              logger.debug("Timeout generating enhanced hash for: {}", imageUrl);
              return null;
          });
    }
    
    /**
     * Calculate difference hash with custom size
     */
    private String calculateDifferenceHashWithSize(BufferedImage image, int size) {
        try {
            BufferedImage thumbnail = resizeImage(image, size + 1, size);
            BufferedImage grayscale = convertToGrayscale(thumbnail);
            
            StringBuilder hash = new StringBuilder();
            
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int leftPixel = grayscale.getRGB(x, y) & 0xFF;
                    int rightPixel = grayscale.getRGB(x + 1, y) & 0xFF;
                    hash.append(leftPixel > rightPixel ? "1" : "0");
                }
            }
            
            return binaryToHex(hash.toString());
            
        } catch (Exception e) {
            logger.debug("Error calculating difference hash: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Batch process multiple images for comparison
     */
    public CompletableFuture<Map<String, String>> batchGenerateHashes(java.util.List<String> imageUrls) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> hashes = new HashMap<>();
            
            imageUrls.parallelStream().forEach(url -> {
                try {
                    String hash = generatePerceptualHash(url).get(10, TimeUnit.SECONDS);
                    if (hash != null) {
                        hashes.put(url, hash);
                    }
                } catch (Exception e) {
                    logger.debug("Failed to generate hash for batch processing: {}", url);
                }
            });
            
            return hashes;
        });
    }

    /**
     * Calculate similarity between two perceptual hashes
     * Returns a value between 0.0 (completely different) and 1.0 (identical)
     */
    public double calculateSimilarity(String hash1, String hash2) {
        if (hash1 == null || hash2 == null) {
            return 0.0;
        }
        
        if (hash1.equals(hash2)) {
            return 1.0;
        }
        
        try {
            int hammingDistance = calculateHammingDistance(hash1, hash2);
            int maxDistance = hash1.length() * 4; // Each hex character represents 4 bits
            
            // Convert hamming distance to similarity score
            double similarity = 1.0 - ((double) hammingDistance / maxDistance);
            
            // Apply threshold - images with >75% similarity are considered very similar
            return Math.max(0.0, similarity);
            
        } catch (Exception e) {
            logger.debug("Error calculating hash similarity: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Check if two images are visually similar based on their hashes
     * Returns true if similarity is above threshold (80%)
     */
    public boolean areImagesSimilar(String hash1, String hash2) {
        double similarity = calculateSimilarity(hash1, hash2);
        return similarity >= 0.80; // 80% similarity threshold
    }    /**
     * Download image from URL with timeout protection
     * Enhanced with better error handling and user agent
     */
    private BufferedImage downloadImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            
            // Set a realistic user agent to avoid blocking
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            connection.setRequestProperty("Cache-Control", "no-cache");
            
            BufferedImage image = ImageIO.read(connection.getInputStream());
            
            if (image == null) {
                logger.debug("Failed to read image from URL (null image): {}", imageUrl);
                return null;
            }
            
            // Validate image dimensions
            if (image.getWidth() < 10 || image.getHeight() < 10) {
                logger.debug("Image too small ({}x{}): {}", image.getWidth(), image.getHeight(), imageUrl);
                return null;
            }
            
            logger.trace("Successfully downloaded image ({}x{}) from: {}", 
                image.getWidth(), image.getHeight(), imageUrl);
            return image;
            
        } catch (IOException e) {
            logger.debug("Failed to download image: {} - {}", imageUrl, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.debug("Unexpected error downloading image: {} - {}", imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Calculate perceptual hash using difference hash (dHash) algorithm
     * This is robust against scaling, rotation, and minor color changes
     */
    private String calculatePerceptualHash(BufferedImage image) {
        try {
            // Step 1: Resize to small thumbnail
            BufferedImage thumbnail = resizeImage(image, HASH_SIZE + 1, HASH_SIZE);
            
            // Step 2: Convert to grayscale
            BufferedImage grayscale = convertToGrayscale(thumbnail);
            
            // Step 3: Calculate difference hash
            StringBuilder hash = new StringBuilder();
            
            for (int y = 0; y < HASH_SIZE; y++) {
                for (int x = 0; x < HASH_SIZE; x++) {
                    int leftPixel = grayscale.getRGB(x, y) & 0xFF;
                    int rightPixel = grayscale.getRGB(x + 1, y) & 0xFF;
                    
                    // Compare adjacent pixels
                    hash.append(leftPixel > rightPixel ? "1" : "0");
                }
            }
            
            // Convert binary string to hexadecimal
            return binaryToHex(hash.toString());
            
        } catch (Exception e) {
            logger.debug("Error calculating perceptual hash: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Resize image to specified dimensions
     */
    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        
        // Use high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();
        
        return resized;
    }

    /**
     * Convert image to grayscale
     */
    private BufferedImage convertToGrayscale(BufferedImage original) {
        BufferedImage grayscale = new BufferedImage(
            original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        
        Graphics2D g2d = grayscale.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();
        
        return grayscale;
    }

    /**
     * Convert binary string to hexadecimal
     */
    private String binaryToHex(String binary) {
        StringBuilder hex = new StringBuilder();
        
        // Pad to multiple of 4
        while (binary.length() % 4 != 0) {
            binary = "0" + binary;
        }
        
        // Convert every 4 bits to hex
        for (int i = 0; i < binary.length(); i += 4) {
            String fourBits = binary.substring(i, i + 4);
            int decimal = Integer.parseInt(fourBits, 2);
            hex.append(Integer.toHexString(decimal));
        }
        
        return hex.toString();
    }

    /**
     * Calculate Hamming distance between two hex strings
     */
    private int calculateHammingDistance(String hex1, String hex2) {
        if (hex1.length() != hex2.length()) {
            return Math.max(hex1.length(), hex2.length()) * 4; // Maximum possible distance
        }
        
        int distance = 0;
        
        for (int i = 0; i < hex1.length(); i++) {
            int val1 = Character.digit(hex1.charAt(i), 16);
            int val2 = Character.digit(hex2.charAt(i), 16);
            
            // XOR and count set bits
            int xor = val1 ^ val2;
            distance += Integer.bitCount(xor);
        }
        
        return distance;
    }
}
