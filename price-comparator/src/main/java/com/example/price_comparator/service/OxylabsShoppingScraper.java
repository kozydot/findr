package com.example.price_comparator.service;

import com.example.price_comparator.dto.ShoppingProduct;
import com.example.price_comparator.model.SpecificationInfo;
import com.example.price_comparator.utils.JsonUtils;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OxylabsShoppingScraper {    private static final Logger logger = LoggerFactory.getLogger(OxylabsShoppingScraper.class);
    
    // Enhanced executor service for parallel processing - optimized for faster enhancement
    private final ExecutorService executorService = Executors.newFixedThreadPool(8); // Process 8 products concurrently for maximum speed

    public List<ShoppingProduct> scrapeShoppingResults(String query, String geoLocation, String username, String password, BiConsumer<Integer, String> progressCallback) {
        List<ShoppingProduct> products = new ArrayList<>();
        
        logger.info("=====================================");
        logger.info("OXYLABS SCRAPING INITIATED");
        logger.info("=====================================");
        logger.info("Request Configuration:");
        logger.info("  Query: '{}'", query);
        logger.info("  Geo Location: '{}'", geoLocation);
        logger.info("  Username: '{}'", username != null ? username.substring(0, Math.min(3, username.length())) + "***" : "null");
        logger.info("  Password: {}", password != null ? "[PROVIDED]" : "[NOT PROVIDED]");
        logger.info("=====================================");
        
        try {
        if (progressCallback != null) {
            progressCallback.accept(10, "Initiating scraping process...");
        }
            
            // Build request payload with detailed logging
            logger.info("BUILDING REQUEST PAYLOAD:");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("source", "google_shopping_search");
            jsonObject.put("geo_location", geoLocation);
            jsonObject.put("query", query);
            jsonObject.put("parse", true);

            JSONArray context = new JSONArray();
            JSONObject resultsLanguage = new JSONObject();
            resultsLanguage.put("key", "results_language");
            resultsLanguage.put("value", "en");
            context.put(resultsLanguage);

            jsonObject.put("context", context);
            
            logger.info("  Source: google_shopping_search");
            logger.info("  Parse enabled: true");
            logger.info("  Results language: en");
            logger.debug("Complete Request Payload:");            logger.debug("{}", formatJsonForLogging(jsonObject.toString()));

            logger.info("PREPARING HTTP CLIENT:");
            logger.info("  Endpoint: https://realtime.oxylabs.io/v1/queries");
            logger.info("  Method: POST");
            logger.info("  Authentication: Basic Auth");
            logger.info("  Timeout: 180 seconds");

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        String credential = Credentials.basic(username, password);
                        Request originalRequest = chain.request();
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", credential)
                                .build();
                        return chain.proceed(newRequest);
                    })
                    .readTimeout(180, TimeUnit.SECONDS)
                    .build();

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(jsonObject.toString(), mediaType);

            Request request = new Request.Builder()
                    .url("https://realtime.oxylabs.io/v1/queries")
                    .post(body)
                    .build();

            logger.info("SENDING REQUEST TO OXYLABS:");
            logger.info("  URL: {}", request.url());
            logger.info("  Payload size: {} characters", jsonObject.toString().length());
            if (progressCallback != null) {
                progressCallback.accept(25, "Sending request to Oxylabs...");
            }
            
            long requestStartTime = System.currentTimeMillis();
              try (Response response = client.newCall(request).execute()) {
                long responseTime = System.currentTimeMillis() - requestStartTime;
                
                logger.info("RESPONSE RECEIVED FROM OXYLABS:");
                logger.info("  Status Code: {} - {}", response.code(), response.message());
                logger.info("  Response Time: {}ms", responseTime);
                logger.info("  Content Type: {}", response.header("Content-Type"));
                logger.debug("Response Headers:");
                response.headers().toMultimap().forEach((name, values) -> 
                    logger.debug("    {}: {}", name, String.join(", ", values)));
                
                if (response.isSuccessful() && response.body() != null) {
                    if (progressCallback != null) {
                        progressCallback.accept(50, "Response received, parsing results...");
                    }
                    String responseBody = response.body().string();
                    
                    logger.info("Response body size: {} characters", responseBody.length());
                    logger.debug("Full Response Body:");
                    logger.debug("{}", formatJsonForLogging(responseBody));
                    
                    JSONObject responseJson = new JSONObject(responseBody);
                    
                    // Log response structure analysis
                    logger.info("Response Structure Analysis:");
                    analyzeResponseStructure(responseJson);
                    
                    JSONArray results = responseJson.optJSONArray("results");
                    if (results != null && results.length() > 0) {
                        logger.info("Processing {} result objects from response", results.length());
                        extractProducts(results, products);
                        if (progressCallback != null) {
                            progressCallback.accept(90, "Found " + products.size() + " potential offers.");
                        }
                        logger.info("EXTRACTION SUMMARY:");
                        logger.info("  Total result objects processed: {}", results.length());
                        logger.info("  Valid products extracted: {}", products.size());
                        double successRate = results.length() > 0 ? (products.size() * 100.0 / results.length()) : 0.0;
                        logger.info("  Extraction success rate: {}%", String.format("%.1f", successRate));
                    } else {
                        logger.warn("No results array found in response");
                        logger.debug("Available response keys: {}", responseJson.keySet());
                        if (progressCallback != null) {
                            progressCallback.accept(90, "No offers found in the initial response.");
                        }
                    }                } else {
                    logger.error("OXYLABS REQUEST FAILED");
                    logger.error("  Status Code: {}", response.code());
                    logger.error("  Status Message: {}", response.message());
                    logger.error("  Request URL: {}", request.url());
                    logger.error("  Request Method: {}", request.method());
                    
                    if (response.body() != null) {
                        String errorBody = response.body().string();
                        logger.error("  Error Response Body Length: {} characters", errorBody.length());
                        logger.error("  Error Response Content:");
                        logger.error("{}", formatJsonForLogging(errorBody));
                    } else {
                        logger.error("  No response body available");
                    }
                    
                    if (progressCallback != null) {
                        progressCallback.accept(100, "Failed to retrieve offers - HTTP " + response.code());
                    }
                }
            }        } catch (Exception e) {
            logger.error("OXYLABS SCRAPING ERROR OCCURRED");
            logger.error("  Exception Type: {}", e.getClass().getSimpleName());
            logger.error("  Exception Message: {}", e.getMessage());
            logger.error("  Query: '{}'", query);
            logger.error("  Geo Location: '{}'", geoLocation);
            logger.error("  Products collected before error: {}", products.size());
            logger.error("  Full Exception Details:", e);
            if (progressCallback != null) {
                progressCallback.accept(100, "Error occurred during scraping: " + e.getClass().getSimpleName());
            }
        }
          if (products.isEmpty()) {
            logger.warn("=====================================");
            logger.warn("SCRAPING COMPLETED WITH NO RESULTS");
            logger.warn("=====================================");
            logger.warn("Query: '{}'", query);
            logger.warn("Geo Location: '{}'", geoLocation);
            logger.warn("Possible reasons:");
            logger.warn("  - No products match the search query");
            logger.warn("  - All products were filtered out (e.g., Google links)");
            logger.warn("  - API response structure changed");
            logger.warn("  - Network or authentication issues");
            logger.warn("=====================================");
        } else {
            logger.info("=====================================");
            logger.info("SCRAPING COMPLETED SUCCESSFULLY");
            logger.info("=====================================");
            logger.info("Final Results Summary:");
            logger.info("  Total products found: {}", products.size());
            logger.info("  Query: '{}'", query);
            logger.info("  Geo Location: '{}'", geoLocation);
            
            // Log some sample products for verification
            int sampleCount = Math.min(3, products.size());
            logger.info("Sample Products (first {}):", sampleCount);
            for (int i = 0; i < sampleCount; i++) {
                ShoppingProduct sample = products.get(i);
                logger.info("  {}. '{}' - {} AED from {}", 
                    i + 1,
                    sample.getTitle().length() > 40 ? sample.getTitle().substring(0, 37) + "..." : sample.getTitle(),
                    String.format("%.2f", sample.getPrice()),
                    sample.getSeller() != null ? sample.getSeller() : "Unknown");
            }
            logger.info("=====================================");
        }
       
        if (progressCallback != null) {
            progressCallback.accept(100, "Comparison finished.");
        }
        return products;
    }    private void extractProducts(JSONArray jsonArray, List<ShoppingProduct> products) {
        if (jsonArray == null) {
            logger.debug("Received null JSON array for product extraction");
            return;
        }
        
        long extractionStartTime = System.currentTimeMillis();
        
        logger.info("PRODUCT EXTRACTION STARTED");
        logger.info("Processing {} items from JSON array", jsonArray.length());
        
        int validProducts = 0;
        int skippedItems = 0;
        int nestedProcessing = 0;
        
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.optJSONObject(i);
            if (item == null) {
                skippedItems++;
                logger.debug("Skipping null item at index {}", i);
                continue;
            }

            logger.debug("Processing item {} of {}", i + 1, jsonArray.length());
            logger.trace("Item structure: {}", item.keySet());

            // Check if this object is a product
            String title = JsonUtils.optString(item, "title", null);
            String url = JsonUtils.optString(item, "url", null);

            if (title != null && url != null && !url.isEmpty()) {
                logger.debug("Found valid product candidate:");
                logger.debug("  Title: '{}'", title);
                logger.debug("  URL: '{}'", url);
                  ShoppingProduct product = new ShoppingProduct();
                product.setTitle(title);                // Enhanced price extraction with detailed logging
                PriceInfo priceInfo = extractPriceInfo(item);
                product.setPrice(priceInfo.value);

                // Enhanced merchant/seller processing
                MerchantInfo merchantInfo = extractMerchantInfo(item);
                String sellerName = merchantInfo.name;
                String sellerSource = merchantInfo.source;
                String finalUrl = merchantInfo.url != null ? merchantInfo.url : url;
                
                // Special debugging for problematic retailers
                if (priceInfo.value == 0.0 || (sellerName != null && sellerName.toLowerCase().contains("noon"))) {
                    logger.warn("PRICE DEBUG for {}: Price={}, Source={}, Currency={}", 
                        sellerName != null ? sellerName : "Unknown Seller", 
                        priceInfo.value, priceInfo.source, priceInfo.currency);
                    logger.warn("PRICE DEBUG - Available keys in item: {}", item.keySet());
                    
                    // Log specific price-related fields
                    String[] priceFields = {"price", "price_str", "current_price", "sale_price", "offer_price", "cost", "amount", "value"};
                    for (String field : priceFields) {
                        if (item.has(field)) {
                            logger.warn("PRICE DEBUG - {}: {}", field, item.opt(field));
                        }
                    }
                }
                
                logger.debug("  Price: {} {} (source: {})", priceInfo.value, priceInfo.currency, priceInfo.source);

                logger.debug("  Seller: '{}' (source: {})", sellerName, sellerSource);
                logger.debug("  Final URL: '{}'", finalUrl);

                // Skip Google links
                if (finalUrl.contains("google.com")) {
                    logger.debug("Skipping product with Google link: {}", finalUrl);
                    skippedItems++;
                    continue;
                }                product.setSeller(sellerName);
                product.setProductLink(finalUrl);
                  // Enhanced image extraction with multiple sources
                String imageUrl = extractBestImageUrl(item);
                product.setImageUrl(imageUrl);
                logger.debug("  Image URL: '{}' (extracted using enhanced logic)", imageUrl);
                
                // If no image found in initial extraction, flag for potential enhancement
                boolean needsImageEnhancement = (imageUrl == null || imageUrl.trim().isEmpty());
                if (needsImageEnhancement) {
                    logger.debug("  Product flagged for image enhancement - no suitable image found in initial extraction");
                }
                  product.setDescription(JsonUtils.optString(item, "description", null));

                // Enhanced specifications processing with multiple extraction strategies
                List<SpecificationInfo> specifications = new ArrayList<>();
                try {
                    specifications = extractEnhancedSpecifications(item, title);
                    logger.debug("  Specifications: {} items found using enhanced extraction", specifications.size());
                } catch (Exception e) {
                    logger.warn("Failed to extract specifications for product '{}': {}", 
                        title.length() > 30 ? title.substring(0, 27) + "..." : title, e.getMessage());
                    logger.debug("Full error details for specification extraction:", e);
                    // Continue with empty specifications list
                }
                product.setSpecifications(specifications);
                
                // Log specifications for debugging
                if (!specifications.isEmpty()) {
                    logger.debug("  Specification details:");
                    for (int specIdx = 0; specIdx < Math.min(specifications.size(), 5); specIdx++) {
                        SpecificationInfo spec = specifications.get(specIdx);
                        logger.debug("    {}: {}", spec.getName(), spec.getValue());
                    }
                    if (specifications.size() > 5) {
                        logger.debug("    ... and {} more specifications", specifications.size() - 5);
                    }
                }products.add(product);
                validProducts++;                logger.info("PRODUCT EXTRACTED [{}]: '{}' from {} at {} {} ({})", 
                    validProducts, 
                    title.length() > 50 ? title.substring(0, 47) + "..." : title,
                    sellerName != null ? sellerName : "Unknown Seller",
                    String.format("%.2f", priceInfo.value), 
                    priceInfo.currency,
                    priceInfo.source);
                    
            } else {
                skippedItems++;
                // If not a product, check for nested arrays to parse
                logger.debug("Item at index {} is not a direct product (missing title or URL)", i);
                logger.debug("Available keys: {}", item.keySet());
                logger.debug("Checking for nested product arrays...");
                
                boolean foundNested = false;
                
                if (item.has("items")) {
                    JSONArray nestedItems = item.optJSONArray("items");
                    if (nestedItems != null && nestedItems.length() > 0) {
                        logger.debug("Found {} items in 'items' array", nestedItems.length());
                        extractProducts(nestedItems, products);
                        nestedProcessing++;
                        foundNested = true;
                    }
                }
                if (item.has("pla")) {
                    JSONArray plaItems = item.optJSONArray("pla");
                    if (plaItems != null && plaItems.length() > 0) {
                        logger.debug("Found {} items in 'pla' array", plaItems.length());
                        extractProducts(plaItems, products);
                        nestedProcessing++;
                        foundNested = true;
                    }
                }
                if (item.has("organic")) {
                    JSONArray organicItems = item.optJSONArray("organic");
                    if (organicItems != null && organicItems.length() > 0) {
                        logger.debug("Found {} items in 'organic' array", organicItems.length());
                        extractProducts(organicItems, products);
                        nestedProcessing++;
                        foundNested = true;
                    }
                }
                if (item.has("content")) {
                    JSONObject content = item.optJSONObject("content");
                    if (content != null && content.has("results")) {
                        JSONObject results = content.optJSONObject("results");
                        if (results != null) {
                            logger.debug("Found content.results structure");
                            if (results.has("pla")) {
                                JSONArray contentPla = results.optJSONArray("pla");
                                if (contentPla != null && contentPla.length() > 0) {
                                    logger.debug("Found {} items in content.results.pla", contentPla.length());
                                    extractProducts(contentPla, products);
                                    nestedProcessing++;
                                    foundNested = true;
                                }
                            }
                            if (results.has("organic")) {
                                JSONArray contentOrganic = results.optJSONArray("organic");
                                if (contentOrganic != null && contentOrganic.length() > 0) {
                                    logger.debug("Found {} items in content.results.organic", contentOrganic.length());
                                    extractProducts(contentOrganic, products);
                                    nestedProcessing++;
                                    foundNested = true;
                                }
                            }
                        }
                    }
                }
                
                if (!foundNested) {
                    logger.trace("No nested arrays found in item: {}", formatJsonForLogging(item.toString()));
                }
            }        }
        
        long extractionEndTime = System.currentTimeMillis();
        long extractionDuration = extractionEndTime - extractionStartTime;
        
        logger.info("PRODUCT EXTRACTION COMPLETED");
        logger.info("Summary for this array:");
        logger.info("  Total items processed: {}", jsonArray.length());
        logger.info("  Valid products extracted: {}", validProducts);
        logger.info("  Items skipped: {}", skippedItems);
        logger.info("  Nested arrays processed: {}", nestedProcessing);
        logger.info("  Extraction time: {}ms", extractionDuration);
        if (jsonArray.length() > 0) {
            double successRate = (validProducts * 100.0 / jsonArray.length());
            logger.info("  Success rate: {}%", String.format("%.1f", successRate));
        }    }private double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) {
            logger.trace("Price parsing: Input string is null or empty");
            return 0.0;
        }
        
        logger.debug("Price parsing: Input string = '{}'", priceStr);
        
        try {
            // Strategy 1: Handle common price formats and prefixes
            String cleanedPrice = priceStr.trim().toLowerCase();
            
            // Remove common price-related text and currency symbols
            cleanedPrice = cleanedPrice
                .replaceAll("price:|from\\s+|starting\\s+|was\\s+|now\\s+|save\\s+", "") // Remove price text
                .replaceAll("aed|usd|eur|gbp|sar|qar|dh|sr", "") // Remove currency codes
                .replaceAll("dirham|riyal|dollar|euro|pound", "") // Remove currency names
                .trim();
            
            logger.trace("Price parsing: After text cleanup = '{}'", cleanedPrice);
            
            // Strategy 2: Handle price ranges - take the first (usually lower) price
            if (cleanedPrice.contains("-") || cleanedPrice.contains("to") || cleanedPrice.contains("~")) {
                String[] parts = cleanedPrice.split("[-~]|\\s+to\\s+");
                if (parts.length >= 1 && !parts[0].trim().isEmpty()) {
                    cleanedPrice = parts[0].trim();
                    logger.trace("Price parsing: Using first price from range = '{}'", cleanedPrice);
                }
            }
            
            // Strategy 3: Extract first valid number using enhanced regex
            // This pattern handles: 123, 123.45, 1,234.56, 1.234,56 (European), etc.
            Pattern pattern = Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)");
            Matcher matcher = pattern.matcher(cleanedPrice);
            
            String bestMatch = null;
            double bestPrice = 0.0;
            
            while (matcher.find()) {
                String priceCandidate = matcher.group(0);
                logger.trace("Price parsing: Found candidate = '{}'", priceCandidate);
                
                try {
                    // Normalize the price string
                    String normalizedPrice = normalizePriceString(priceCandidate);
                    double parsedPrice = Double.parseDouble(normalizedPrice);
                    
                    // Prefer higher prices (more likely to be the actual price vs SKU numbers)
                    if (parsedPrice > bestPrice && parsedPrice < 1000000) { // Sanity check
                        bestPrice = parsedPrice;
                        bestMatch = priceCandidate;
                    }
                } catch (NumberFormatException e) {
                    logger.trace("Price parsing: Failed to parse candidate '{}': {}", priceCandidate, e.getMessage());
                }
            }
            
            if (bestMatch != null && bestPrice > 0) {
                logger.debug("Price parsing: Successfully extracted {} from '{}' (candidate: '{}')", bestPrice, priceStr, bestMatch);
                return bestPrice;
            } else {
                logger.warn("Price parsing: No valid numeric pattern found in '{}' (cleaned: '{}')", priceStr, cleanedPrice);
            }
        } catch (Exception e) {
            logger.error("Price parsing: Unexpected error while parsing '{}': {}", priceStr, e.getMessage(), e);
        }
        
        logger.warn("Price parsing: Could not extract valid price from '{}'", priceStr);
        return 0.0;
    }
    
    /**
     * Normalize price string to handle different decimal and thousands separators
     */
    private String normalizePriceString(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) {
            return "0";
        }
        
        // Handle different decimal separator conventions
        if (priceStr.contains(",") && priceStr.contains(".")) {
            // European format: 1.234,56 -> 1234.56
            int lastComma = priceStr.lastIndexOf(",");
            int lastDot = priceStr.lastIndexOf(".");
            
            if (lastComma > lastDot) {
                // Comma is decimal separator
                return priceStr.replaceAll("\\.", "").replace(",", ".");
            } else {
                // Dot is decimal separator
                return priceStr.replaceAll(",", "");
            }
        } else if (priceStr.contains(",")) {
            // Check if comma is thousands separator or decimal separator
            int commaIndex = priceStr.lastIndexOf(",");
            String afterComma = priceStr.substring(commaIndex + 1);
            
            if (afterComma.length() <= 2 && afterComma.matches("\\d{1,2}")) {
                // Likely decimal separator: 12,34 -> 12.34
                return priceStr.replace(",", ".");
            } else {
                // Likely thousands separator: 1,234 -> 1234
                return priceStr.replaceAll(",", "");
            }
        }
        
        return priceStr;
    }
      /**
     * Formats JSON string for better readability in logs
     */
    private String formatJsonForLogging(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return "[EMPTY JSON]";
        }
        
        try {
            // Try to parse as JSONObject first
            JSONObject jsonObject = new JSONObject(jsonString);
            return jsonObject.toString(2); // Pretty print with 2-space indentation
        } catch (Exception e1) {
            try {
                // If object parsing fails, try as JSONArray
                JSONArray jsonArray = new JSONArray(jsonString);
                return jsonArray.toString(2);
            } catch (Exception e2) {
                // If both fail, return original with truncation for very long strings
                logger.debug("Failed to format JSON for logging: Object error: {}, Array error: {}", 
                    e1.getMessage(), e2.getMessage());
                
                if (jsonString.length() > 1000) {
                    return jsonString.substring(0, 997) + "...";
                }
                return jsonString;
            }
        }
    }
      /**
     * Analyzes and logs the structure of the response JSON
     */
    private void analyzeResponseStructure(JSONObject responseJson) {
        try {
            logger.info("  Top-level keys: {}", responseJson.keySet());
            
            if (responseJson.has("results")) {
                JSONArray results = responseJson.optJSONArray("results");
                if (results != null) {
                    logger.info("  Results array length: {}", results.length());
                    
                    // Analyze first result structure if available
                    if (results.length() > 0) {
                        JSONObject firstResult = results.optJSONObject(0);
                        if (firstResult != null) {
                            logger.info("  First result structure:");
                            logger.info("    Keys: {}", firstResult.keySet());
                            
                            // Check for nested content structures
                            if (firstResult.has("content")) {
                                JSONObject content = firstResult.optJSONObject("content");
                                if (content != null) {
                                    logger.info("    Content keys: {}", content.keySet());
                                    
                                    if (content.has("results")) {
                                        JSONObject contentResults = content.optJSONObject("results");
                                        if (contentResults != null) {
                                            logger.info("    Content results keys: {}", contentResults.keySet());
                                            

                                            // Log counts for different product array types
                                            if (contentResults.has("pla")) {
                                                JSONArray pla = contentResults.optJSONArray("pla");
                                                if (pla != null) {
                                                    logger.info("    PLA products count: {}", pla.length());
                                                }
                                            }
                                            if (contentResults.has("organic")) {
                                                JSONArray organic = contentResults.optJSONArray("organic");
                                                if (organic != null) {
                                                    logger.info("    Organic products count: {}", organic.length());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            

                            // Check for direct product arrays in first result
                            if (firstResult.has("pla")) {
                                JSONArray pla = firstResult.optJSONArray("pla");
                                if (pla != null) {
                                    logger.info("    Direct PLA products count: {}", pla.length());
                                }
                            }
                            if (firstResult.has("organic")) {
                                JSONArray organic = firstResult.optJSONArray("organic");
                                if (organic != null) {
                                    logger.info("    Direct organic products count: {}", organic.length());
                                }
                            }
                        }
                    }
                } else {
                    logger.warn("  Results key exists but is not an array");
                }
            } else {
                logger.warn("  No 'results' key found in response");
            }
            
            // Check for error indicators
            if (responseJson.has("error")) {
                logger.error("  Error found in response: {}", responseJson.optString("error"));
            }
            
            if (responseJson.has("status")) {
                logger.info("  Response status: {}", responseJson.optString("status"));
            }
            
            // Log additional metadata if available
            if (responseJson.has("job_id")) {
                logger.info("  Job ID: {}", responseJson.optString("job_id"));
            }
            
            if (responseJson.has("created_at")) {
                logger.info("  Created at: {}", responseJson.optString("created_at"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to analyze response structure: {}", e.getMessage());
        }
    }    /**
     * Enhanced image URL extraction with multiple fallback strategies and quality selection
     * Updated to handle full Oxylabs Shopping Product API response structure
     */
    private String extractBestImageUrl(JSONObject item) {
        String imageUrl = null;
        String source = "none";
        int bestQuality = 0;
        
        // Strategy 1: Check for Oxylabs standard product image field first (highest priority)
        if (item.has("image") && !item.isNull("image")) {
            Object imageField = item.opt("image");
            if (imageField instanceof String) {
                String candidateUrl = (String) imageField;
                if (isValidImageUrl(candidateUrl)) {
                    imageUrl = candidateUrl;
                    source = "image (primary)";
                    bestQuality = estimateImageQuality(candidateUrl) + 2000000; // Highest priority
                }
            } else if (imageField instanceof JSONObject) {
                JSONObject imageObj = (JSONObject) imageField;
                String candidateUrl = JsonUtils.optString(imageObj, "url", null);
                if (candidateUrl == null) candidateUrl = JsonUtils.optString(imageObj, "src", null);
                if (candidateUrl == null) candidateUrl = JsonUtils.optString(imageObj, "href", null);
                if (isValidImageUrl(candidateUrl)) {
                    imageUrl = candidateUrl;
                    source = "image object (primary)";
                    int width = imageObj.optInt("width", 0);
                    int height = imageObj.optInt("height", 0);
                    bestQuality = (width * height) + 2000000; // Highest priority
                }
            }
        }
        
        // Strategy 2: Check for main_image field (Oxylabs Product API specific)
        if (imageUrl == null && item.has("main_image") && !item.isNull("main_image")) {
            String candidateUrl = JsonUtils.optString(item, "main_image", null);
            if (isValidImageUrl(candidateUrl)) {
                imageUrl = candidateUrl;
                source = "main_image";
                bestQuality = estimateImageQuality(candidateUrl) + 1800000;
            }
        }
        
        // Strategy 3: Check for direct string image fields
        String[] imageFields = {"thumbnail", "image_url", "thumb", "picture", "photo", "img_url", "product_image", "featured_image"};
        
        for (String field : imageFields) {
            if (item.has(field) && !item.isNull(field)) {
                Object fieldValue = item.opt(field);
                if (fieldValue instanceof String) {
                    String candidateUrl = (String) fieldValue;
                    if (isValidImageUrl(candidateUrl)) {
                        if (imageUrl == null) {
                            imageUrl = candidateUrl;
                            source = field;
                            bestQuality = estimateImageQuality(candidateUrl) + 1500000;
                        }
                        break;
                    }
                }
            }
        }        
        // Strategy 4: Check for image arrays and select highest quality
        if (item.has("images") && !item.isNull("images")) {
            JSONArray images = item.optJSONArray("images");
            if (images != null && images.length() > 0) {
                String bestImage = null;
                int currentBestQuality = bestQuality;
                
                for (int i = 0; i < images.length(); i++) {
                    Object imgItem = images.opt(i);
                    String candidateUrl = null;
                    int quality = 0;
                    
                    if (imgItem instanceof String) {
                        candidateUrl = (String) imgItem;
                        quality = estimateImageQuality(candidateUrl) + 1200000;
                    } else if (imgItem instanceof JSONObject) {
                        JSONObject imgObj = (JSONObject) imgItem;
                        candidateUrl = JsonUtils.optString(imgObj, "url", null);
                        if (candidateUrl == null) {
                            candidateUrl = JsonUtils.optString(imgObj, "src", null);
                        }
                        if (candidateUrl == null) {
                            candidateUrl = JsonUtils.optString(imgObj, "href", null);
                        }
                        
                        // Calculate quality based on dimensions
                        int width = imgObj.optInt("width", 0);
                        int height = imgObj.optInt("height", 0);
                        quality = (width * height) + 1200000;
                        
                        // If no dimensions, estimate from URL
                        if (quality == 1200000) {
                            quality = estimateImageQuality(candidateUrl) + 1200000;
                        }
                        
                        // Prefer main product images
                        String type = JsonUtils.optString(imgObj, "type", "");
                        if ("main".equalsIgnoreCase(type) || "primary".equalsIgnoreCase(type)) {
                            quality += 500000; // Boost main images
                        }
                    }
                    
                    if (isValidImageUrl(candidateUrl) && quality > currentBestQuality) {
                        bestImage = candidateUrl;
                        currentBestQuality = quality;
                    }
                }
                
                if (bestImage != null && (imageUrl == null || currentBestQuality > bestQuality)) {
                    imageUrl = bestImage;
                    bestQuality = currentBestQuality;
                    source = "images array (quality: " + (currentBestQuality - 1200000) + ")";
                }
            }
        }
        
        // Strategy 5: Check for product_info nested images (Oxylabs specific)
        if (item.has("product_info") && !item.isNull("product_info")) {
            JSONObject productInfo = item.optJSONObject("product_info");
            if (productInfo != null) {
                String productImage = JsonUtils.optString(productInfo, "image", null);
                if (productImage == null) {
                    productImage = JsonUtils.optString(productInfo, "main_image", null);
                }
                if (productImage == null) {
                    productImage = JsonUtils.optString(productInfo, "primary_image", null);
                }
                if (isValidImageUrl(productImage) && (imageUrl == null || bestQuality < 1000000)) {
                    imageUrl = productImage;
                    source = "product_info image";
                    bestQuality = estimateImageQuality(productImage) + 1000000;
                }
            }
        }
        
        // Strategy 6: Check content object for images
        if (item.has("content") && !item.isNull("content")) {
            JSONObject content = item.optJSONObject("content");
            if (content != null) {
                String contentImage = JsonUtils.optString(content, "image_url", null);
                if (contentImage == null) {
                    contentImage = JsonUtils.optString(content, "featured_image", null);
                }
                if (contentImage == null) {
                    contentImage = JsonUtils.optString(content, "main_image", null);
                }
                if (isValidImageUrl(contentImage) && (imageUrl == null || bestQuality < 800000)) {
                    imageUrl = contentImage;
                    source = "content image";
                    bestQuality = estimateImageQuality(contentImage) + 800000;
                }
            }
        }
        
        // Strategy 7: Check merchant or seller image (lowest priority)
        if (imageUrl == null || bestQuality < 500000) {
            JSONObject merchant = item.optJSONObject("merchant");
            if (merchant != null) {
                String merchantImage = JsonUtils.optString(merchant, "image", null);
                if (merchantImage == null) {
                    merchantImage = JsonUtils.optString(merchant, "logo", null);
                }
                if (isValidImageUrl(merchantImage) && imageUrl == null) {
                    imageUrl = merchantImage;
                    source = "merchant image";
                    bestQuality = estimateImageQuality(merchantImage) + 100000;
                }
            }
        }
        
        logger.debug("Image extraction: Found '{}' from source '{}' (quality: {})", 
            imageUrl != null ? imageUrl : "null", source, bestQuality);
        return imageUrl;
    }
      /**
     * Validate if a URL is a proper image URL
     * Enhanced validation for better product image detection
     */
    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        url = url.toLowerCase();
        
        // Check if it's a valid HTTP/HTTPS URL
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }
        
        // Check for common image extensions
        if (url.contains(".jpg") || url.contains(".jpeg") || url.contains(".png") || 
            url.contains(".gif") || url.contains(".webp") || url.contains(".svg") ||
            url.contains(".bmp") || url.contains(".tiff") || url.contains(".ico")) {
            return true;
        }
        
        // Check for image hosting patterns (Amazon, CDN, etc.)
        if (url.contains("images-amazon.com") || url.contains("media-amazon.com") ||
            url.contains("cloudfront.net") || url.contains("googleapis.com") ||
            url.contains("shopify.com") || url.contains("nooncdn.com") ||
            url.contains("desertcart.ae/products/") || url.contains("/images/") ||
            url.contains("/img/") || url.contains("/photos/") || url.contains("/pic/") ||
            url.contains("imgix.net") || url.contains("fastly.com") ||
            url.contains("cdn") || url.contains("image") || url.contains("thumb") ||
            url.contains("photo") || url.contains("picture")) {
            return true;
        }
        
        // Google Shopping specific image patterns
        if (url.contains("shopping.googleusercontent.com") || 
            url.contains("lh3.googleusercontent.com") ||
            url.contains("ssl-images-amazon.com") ||
            url.contains("m.media-amazon.com")) {
            return true;
        }
        
        // UAE-specific e-commerce platforms
        if (url.contains("noon.com") || url.contains("amazon.ae") || 
            url.contains("carrefouruae.com") || url.contains("sharafdg.com") ||
            url.contains("emirates.com") || url.contains("dubaistore.com")) {
            return true;
        }
        
        return false;
    }
      /**
     * Estimate image quality based on URL patterns
     * Enhanced quality detection for better image selection
     */
    private int estimateImageQuality(String url) {
        if (url == null) return 0;
        
        String lowerUrl = url.toLowerCase();
        int quality = 100; // Base quality
        
        // Look for size indicators in URL (higher resolution = better quality)
        if (lowerUrl.contains("_sl1500_") || lowerUrl.contains("1500x1500") || lowerUrl.contains("1500")) {
            quality = 1500 * 1500;
        } else if (lowerUrl.contains("_sl1200_") || lowerUrl.contains("1200x1200") || lowerUrl.contains("1200")) {
            quality = 1200 * 1200;
        } else if (lowerUrl.contains("_sl800_") || lowerUrl.contains("800x800") || lowerUrl.contains("800")) {
            quality = 800 * 800;
        } else if (lowerUrl.contains("_sl500_") || lowerUrl.contains("500x500") || lowerUrl.contains("500")) {
            quality = 500 * 500;
        } else if (lowerUrl.contains("large") || lowerUrl.contains("big") || lowerUrl.contains("high")) {
            quality = 1000 * 1000;
        } else if (lowerUrl.contains("medium") || lowerUrl.contains("med")) {
            quality = 500 * 500;
        } else if (lowerUrl.contains("small") || lowerUrl.contains("thumb") || lowerUrl.contains("mini")) {
            quality = 200 * 200;
        }
        
        // Boost quality for main product images
        if (lowerUrl.contains("main") || lowerUrl.contains("primary") || lowerUrl.contains("hero")) {
            quality += 200000;
        }
        
        // Boost quality for product-specific image sources
        if (lowerUrl.contains("product") || lowerUrl.contains("item")) {
            quality += 100000;
        }
        
        // Prefer high-quality image hosting services
        if (lowerUrl.contains("images-amazon.com") || lowerUrl.contains("ssl-images-amazon.com")) {
            quality += 150000;
        } else if (lowerUrl.contains("googleusercontent.com")) {
            quality += 100000;
        } else if (lowerUrl.contains("cloudfront.net") || lowerUrl.contains("cdn")) {
            quality += 75000;
        }
        
        return quality;
    }/**
     * Enhanced specification extraction with multiple strategies based on Oxylabs Shopping Product API
     * This method implements comprehensive extraction following the documented API response structure
     */
    private List<SpecificationInfo> extractEnhancedSpecifications(JSONObject item, String productTitle) {
        List<SpecificationInfo> specifications = new ArrayList<>();
        
        logger.debug("Starting enhanced specification extraction for: {}", 
            productTitle != null ? productTitle.substring(0, Math.min(50, productTitle.length())) : "Unknown Product");
        
        // Strategy 1: Extract from Oxylabs standard "specifications" array
        // According to documentation: specifications[].items[].title + value, with section_title
        JSONArray specificationsArray = item.optJSONArray("specifications");
        if (specificationsArray != null) {
            logger.debug("Found Oxylabs specifications array with {} sections", specificationsArray.length());
            
            for (int i = 0; i < specificationsArray.length(); i++) {
                JSONObject specSection = specificationsArray.optJSONObject(i);
                if (specSection != null) {
                    String sectionTitle = JsonUtils.optString(specSection, "section_title", "General");
                    JSONArray items = specSection.optJSONArray("items");
                    
                    if (items != null) {
                        logger.debug("Processing specification section '{}' with {} items", sectionTitle, items.length());
                        
                        for (int j = 0; j < items.length(); j++) {
                            JSONObject specItem = items.optJSONObject(j);
                            if (specItem != null) {
                                String title = JsonUtils.optString(specItem, "title", null);
                                String value = JsonUtils.optString(specItem, "value", null);
                                
                                if (title != null && value != null && !title.trim().isEmpty() && !value.trim().isEmpty()) {
                                    // Enhance title with section context if meaningful
                                    String enhancedTitle = enhanceSpecificationTitle(title, sectionTitle);
                                    specifications.add(new SpecificationInfo(enhancedTitle, value.trim()));
                                    logger.trace("Oxylabs spec: {} = {} (section: {})", enhancedTitle, value, sectionTitle);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Strategy 2: Extract from product_details_keywords array (Oxylabs specific)
        JSONArray detailsKeywords = item.optJSONArray("product_details_keywords");
        if (detailsKeywords != null && detailsKeywords.length() > 0) {
            logger.debug("Found product_details_keywords array with {} items", detailsKeywords.length());
            
            List<String> keywords = new ArrayList<>();
            for (int i = 0; i < detailsKeywords.length(); i++) {
                String keyword = detailsKeywords.optString(i);
                if (keyword != null && !keyword.trim().isEmpty()) {
                    keywords.add(keyword.trim());
                }
            }
            
            if (!keywords.isEmpty()) {
                // Group keywords into specifications
                specifications.addAll(extractSpecificationsFromKeywords(keywords));
                logger.debug("Extracted {} specifications from product keywords", keywords.size());
            }
        }
        
        // Strategy 3: Legacy specifications array for backward compatibility
        JSONArray legacySpecs = item.optJSONArray("specifications");
        if (legacySpecs != null && specificationsArray == null) { // Only if new format not found
            logger.debug("Found legacy specifications array with {} items", legacySpecs.length());
            for (int j = 0; j < legacySpecs.length(); j++) {
                JSONObject specObject = legacySpecs.optJSONObject(j);
                if (specObject != null) {
                    String specName = JsonUtils.optString(specObject, "name", null);
                    String specValue = JsonUtils.optString(specObject, "value", null);
                    if (specName != null && specValue != null) {
                        specifications.add(new SpecificationInfo(specName, specValue));
                        logger.trace("Legacy spec: {} = {}", specName, specValue);
                    }
                }
            }
        }          // Strategy 4: "About this item" or product details section (string and array support)
        String[] detailFields = {"about", "about_this_item", "product_details", "features", "description_long"};
        
        for (String field : detailFields) {
            if (item.has(field) && !item.isNull(field)) {
                Object fieldValue = item.opt(field);
                if (fieldValue instanceof String) {
                    String details = (String) fieldValue;
                    if (!details.isEmpty()) {
                        logger.debug("Found '{}' field with {} characters of content", field, details.length());
                        List<SpecificationInfo> extractedSpecs = parseSpecificationsFromText(details, field);
                        specifications.addAll(extractedSpecs);
                        logger.debug("Extracted {} specifications from '{}' field", extractedSpecs.size(), field);
                    }
                } else if (fieldValue instanceof JSONArray) {
                    // Handle array-based about sections (common in real Google Shopping responses)
                    JSONArray aboutArray = (JSONArray) fieldValue;
                    logger.debug("Found '{}' array with {} items", field, aboutArray.length());
                    
                    StringBuilder combinedAbout = new StringBuilder();
                    for (int i = 0; i < aboutArray.length(); i++) {
                        Object arrayItem = aboutArray.opt(i);
                        if (arrayItem instanceof String) {
                            String aboutItem = (String) arrayItem;
                            if (!aboutItem.trim().isEmpty()) {
                                if (combinedAbout.length() > 0) {
                                    combinedAbout.append("\n");
                                }
                                combinedAbout.append(aboutItem.trim());
                            }
                        }
                    }
                    
                    if (combinedAbout.length() > 0) {
                        List<SpecificationInfo> extractedSpecs = parseSpecificationsFromText(combinedAbout.toString(), field + "_array");
                        specifications.addAll(extractedSpecs);
                        logger.debug("Extracted {} specifications from '{}' array", extractedSpecs.size(), field);
                    }
                }
            }
        }
        
        // Strategy 5: Nested product information objects
        JSONObject productInfo = item.optJSONObject("product_info");
        if (productInfo != null) {
            logger.debug("Found product_info object");
            extractSpecificationsFromObject(productInfo, specifications, "product_info");
        }
        
        JSONObject details = item.optJSONObject("details");
        if (details != null) {
            logger.debug("Found details object");
            extractSpecificationsFromObject(details, specifications, "details");
        }
        
        // Strategy 6: Check for attributes or properties
        JSONObject attributes = item.optJSONObject("attributes");
        if (attributes != null) {
            logger.debug("Found attributes object");
            extractSpecificationsFromObject(attributes, specifications, "attributes");
        }
          // Strategy 7: Extract from nested content structures
        JSONObject content = item.optJSONObject("content");
        if (content != null) {
            logger.debug("Found content object, checking for specifications");
            
            // Check content.specifications
            JSONArray contentSpecs = content.optJSONArray("specifications");
            if (contentSpecs != null) {
                logger.debug("Found content.specifications array with {} items", contentSpecs.length());
                for (int i = 0; i < contentSpecs.length(); i++) {
                    JSONObject spec = contentSpecs.optJSONObject(i);
                    if (spec != null) {
                        String name = JsonUtils.optString(spec, "name", null);
                        String value = JsonUtils.optString(spec, "value", null);
                        if (name != null && value != null) {
                            specifications.add(new SpecificationInfo(name, value));
                        }
                    }
                }
            }            // Check for product details in content
            if (content.has("product_details") && !content.isNull("product_details")) {
                Object detailsValue = content.opt("product_details");
                if (detailsValue instanceof String) {
                    String contentDetails = (String) detailsValue;
                    List<SpecificationInfo> detailSpecs = parseSpecificationsFromText(contentDetails, "content.product_details");
                    specifications.addAll(detailSpecs);
                }
            }
        }
          // Strategy 8: Extract common product attributes from various fields
        try {
            extractCommonProductAttributes(item, specifications);
        } catch (Exception e) {
            logger.warn("Failed to extract common product attributes: {}", e.getMessage());
            logger.debug("Full error details for attribute extraction:", e);
        }
        
        // Remove duplicates and clean up
        try {
            specifications = removeSpecificationDuplicates(specifications);
        } catch (Exception e) {
            logger.warn("Failed to remove specification duplicates: {}", e.getMessage());
            logger.debug("Full error details for deduplication:", e);
        }
        
        logger.debug("Enhanced specification extraction complete: {} unique specifications found", specifications.size());
        return specifications;
    }
      /**
     * Parse specifications from free-text content (like "About this item")
     * Enhanced to handle various text formats and extract more specifications
     */
    private List<SpecificationInfo> parseSpecificationsFromText(String text, String source) {
        List<SpecificationInfo> specs = new ArrayList<>();
        
        if (text == null || text.trim().isEmpty()) {
            return specs;
        }
        
        logger.trace("Parsing specifications from text (source: {}): {}", source, 
            text.length() > 100 ? text.substring(0, 97) + "..." : text);
        
        // Split text into lines and look for key-value patterns
        String[] lines = text.split("\\n|\\r\\n|\\r|;");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Pattern 1: "Key: Value" or "Key - Value"
            if (line.contains(":") || line.contains(" - ")) {
                String[] parts;
                if (line.contains(":")) {
                    parts = line.split(":", 2);
                } else {
                    parts = line.split(" - ", 2);
                }
                  if (parts.length == 2) {
                    String key = normalizeSpecificationKey(parts[0].trim());
                    String value = parts[1].trim();
                    
                    if (!key.isEmpty() && !value.isEmpty() && key.length() < 100 && value.length() < 500) {
                        specs.add(new SpecificationInfo(key, value));
                        logger.trace("Parsed spec: {} = {}", key, value);
                    }
                }
            }
            
            // Pattern 2: Look for bullet points with specifications
            else if (line.startsWith("•") || line.startsWith("*") || line.startsWith("-")) {
                String cleanLine = line.substring(1).trim();
                if (cleanLine.contains(":")) {
                    String[] parts = cleanLine.split(":", 2);                    if (parts.length == 2) {
                        String key = normalizeSpecificationKey(parts[0].trim());
                        String value = parts[1].trim();
                        
                        if (!key.isEmpty() && !value.isEmpty()) {
                            specs.add(new SpecificationInfo(key, value));
                            logger.trace("Parsed bullet spec: {} = {}", key, value);
                        }
                    }
                } else if (cleanLine.length() > 5 && cleanLine.length() < 200) {
                    // Try to extract technical specs from the feature line
                    List<SpecificationInfo> techSpecs = extractTechnicalSpecifications(cleanLine);
                    if (!techSpecs.isEmpty()) {
                        specs.addAll(techSpecs);
                    } else {
                        // Treat entire line as a feature
                        specs.add(new SpecificationInfo("Feature", cleanLine));
                        logger.trace("Parsed feature: {}", cleanLine);
                    }
                }
            }
            
            // Pattern 3: Lines without bullet points but containing technical specs
            else if (line.length() > 10 && line.length() < 300) {
                List<SpecificationInfo> techSpecs = extractTechnicalSpecifications(line);
                specs.addAll(techSpecs);
            }
        }
        
        // If we didn't find much structured data, try to extract from the whole text
        if (specs.size() < 3) {
            List<SpecificationInfo> techSpecs = extractTechnicalSpecifications(text);
            specs.addAll(techSpecs);
        }
        
        logger.trace("Parsed {} specifications from text (source: {})", specs.size(), source);
        return specs;
    }
    
    /**
     * Extract technical specifications from natural language text
     */
    private List<SpecificationInfo> extractTechnicalSpecifications(String text) {
        List<SpecificationInfo> specs = new ArrayList<>();
        
        if (text == null || text.trim().isEmpty()) {
            return specs;
        }
        
        text = text.toLowerCase().trim();
        
        // Display patterns: "6.8-inch", "13.6 inch", "6.1" display"
        if (text.matches(".*\\d+\\.?\\d*[\\s-]?inch.*")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+\\.?\\d*)[\\s-]?inch");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String size = matcher.group(1) + " inches";
                specs.add(new SpecificationInfo("Display Size", size));
            }
        }
        
        // Storage patterns: "256GB", "1TB", "512 GB"
        if (text.matches(".*\\d+\\s?[gt]b.*")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s?([gt]b)");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String storage = matcher.group(1) + matcher.group(2).toUpperCase();
                if (text.contains("storage") || text.contains("ssd") || text.contains("memory")) {
                    specs.add(new SpecificationInfo("Storage", storage));
                } else if (text.contains("ram")) {
                    specs.add(new SpecificationInfo("RAM", storage));
                }
            }
        }
        
        // RAM patterns: "8GB RAM", "16 GB memory"
        if (text.matches(".*\\d+\\s?gb\\s?(ram|memory).*")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s?gb\\s?(ram|memory)");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String ram = matcher.group(1) + "GB";
                specs.add(new SpecificationInfo("RAM", ram));
            }
        }
        
        // Battery patterns: "5000mAh", "18 hours battery"
        if (text.matches(".*\\d+\\s?mah.*")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s?mah");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                specs.add(new SpecificationInfo("Battery", matcher.group(1) + "mAh"));
            }
        } else if (text.matches(".*\\d+\\s?hour.*battery.*")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s?hour");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                specs.add(new SpecificationInfo("Battery Life", matcher.group(1) + " hours"));
            }
        }
        
        // Camera patterns: "200MP", "12MP main camera"
        if (text.matches(".*\\d+mp.*")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)mp");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String camera = matcher.group(1) + "MP";
                if (text.contains("main") || text.contains("primary")) {
                    camera += " Main";
                } else if (text.contains("ultra") || text.contains("wide")) {
                    camera += " Ultra Wide";
                } else if (text.contains("telephoto")) {
                    camera += " Telephoto";
                }
                specs.add(new SpecificationInfo("Camera", camera));
            }
        }
        
        // Connectivity patterns: "Bluetooth 5.3", "Wi-Fi 6E"
        if (text.contains("bluetooth")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("bluetooth\\s?(\\d+\\.?\\d*)");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                specs.add(new SpecificationInfo("Connectivity", "Bluetooth " + matcher.group(1)));
            }
        }
          if (text.contains("wi-fi") || text.contains("wifi")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("wi-?fi\\s?(\\d+\\w*)");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                specs.add(new SpecificationInfo("Wi-Fi", "Wi-Fi " + matcher.group(1)));
            }
        }
        
        return specs;
    }    /**
     * Enhance specification title with section context when meaningful
     */
    private String enhanceSpecificationTitle(String title, String sectionTitle) {
        if (title == null || title.trim().isEmpty()) {
            return title;
        }
        
        if (sectionTitle == null || sectionTitle.trim().isEmpty() || 
            "General".equalsIgnoreCase(sectionTitle) || 
            title.toLowerCase().contains(sectionTitle.toLowerCase())) {
            return title;
        }
        
        // Add section context for clarity (e.g., "Display" section + "Size" -> "Display Size")
        return sectionTitle + " " + title;
    }
    
    /**
     * Extract specifications from product details keywords (Oxylabs specific feature)
     * Groups related keywords into meaningful specifications
     */
    private List<SpecificationInfo> extractSpecificationsFromKeywords(List<String> keywords) {
        List<SpecificationInfo> specs = new ArrayList<>();
        
        if (keywords == null || keywords.isEmpty()) {
            return specs;
        }
        
        // Group keywords by category
        List<String> materials = new ArrayList<>();
        List<String> features = new ArrayList<>();
        List<String> design = new ArrayList<>();
        List<String> technical = new ArrayList<>();
        
        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase();
            
            // Categorize keywords
            if (lowerKeyword.contains("leather") || lowerKeyword.contains("cotton") || 
                lowerKeyword.contains("metal") || lowerKeyword.contains("plastic") ||
                lowerKeyword.contains("fabric") || lowerKeyword.contains("synthetic")) {
                materials.add(keyword);
            } else if (lowerKeyword.contains("closure") || lowerKeyword.contains("cushioned") ||
                      lowerKeyword.contains("grip") || lowerKeyword.contains("insole") ||
                      lowerKeyword.contains("outsole") || lowerKeyword.contains("lining")) {
                features.add(keyword);
            } else if (lowerKeyword.contains("design") || lowerKeyword.contains("style") ||
                      lowerKeyword.contains("pattern") || lowerKeyword.contains("finish")) {
                design.add(keyword);
            } else if (lowerKeyword.matches(".*\\d+.*") || lowerKeyword.contains("tech") ||
                      lowerKeyword.contains("battery") || lowerKeyword.contains("processor")) {
                technical.add(keyword);
            } else {
                features.add(keyword); // Default to features
            }
        }
        
        // Convert grouped keywords to specifications
        if (!materials.isEmpty()) {
            specs.add(new SpecificationInfo("Materials", String.join(", ", materials)));
        }
        if (!features.isEmpty()) {
            specs.add(new SpecificationInfo("Features", String.join(", ", features)));
        }
        if (!design.isEmpty()) {
            specs.add(new SpecificationInfo("Design", String.join(", ", design)));
        }
        if (!technical.isEmpty()) {
            specs.add(new SpecificationInfo("Technical Details", String.join(", ", technical)));
        }
        
        logger.trace("Grouped {} keywords into {} specification categories", keywords.size(), specs.size());
        return specs;
    }
    
    /**
     * Enhanced merchant extraction following Oxylabs API structure
     */
    private MerchantInfo extractMerchantInfo(JSONObject item) {
        String sellerName = null;
        String sellerUrl = null;
        String sellerSource = "none";
        
        // Check merchant object (Oxylabs standard)
        JSONObject merchant = item.optJSONObject("merchant");
        if (merchant != null) {
            sellerName = JsonUtils.optString(merchant, "name", null);
            sellerUrl = JsonUtils.optString(merchant, "url", null);
            sellerSource = "merchant object";
            logger.debug("Merchant data found: name='{}', url='{}'", sellerName, sellerUrl);
        }
        
        // Fallback to legacy fields
        if (sellerName == null) {
            sellerName = JsonUtils.optString(item, "seller", null);
            if (sellerName != null) {
                sellerSource = "seller field";
            }
        }
        
        // Extract store information if available
        String store = JsonUtils.optString(item, "store", null);
        if (store != null && sellerName == null) {
            sellerName = store;
            sellerSource = "store field";
        }
        
        return new MerchantInfo(sellerName, sellerUrl, sellerSource);
    }
    
    /**
     * Helper class for merchant information
     */
    private static class MerchantInfo {
        final String name;
        final String url;
        final String source;
        
        MerchantInfo(String name, String url, String source) {
            this.name = name;
            this.url = url;
            this.source = source;
        }
    }
      /**
     * Enhanced price extraction with multiple fallback strategies
     */
    private PriceInfo extractPriceInfo(JSONObject item) {
        double price = 0.0;
        String currency = null;
        String priceSource = "none";
        
        logger.trace("Price extraction: Starting for item with keys: {}", item.keySet());
        
        // Strategy 1: Check price object structure
        if (item.has("price")) {
            Object priceObj = item.get("price");
            if (priceObj instanceof JSONObject) {
                JSONObject priceObject = (JSONObject) priceObj;
                Object value = priceObject.opt("value");
                currency = JsonUtils.optString(priceObject, "currency", null);
                
                if (value instanceof Number) {
                    price = ((Number) value).doubleValue();
                    priceSource = "price.value";
                } else if (value instanceof String) {
                    price = parsePrice((String) value);
                    priceSource = "price.value (string)";
                }
                
                // Also check for common price object variations
                if (price == 0.0) {
                    String amount = JsonUtils.optString(priceObject, "amount", null);
                    if (amount != null) {
                        price = parsePrice(amount);
                        priceSource = "price.amount";
                    }
                }
            } else if (priceObj instanceof Number) {
                price = ((Number) priceObj).doubleValue();
                priceSource = "price (number)";
            } else if (priceObj instanceof String) {
                price = parsePrice((String) priceObj);
                priceSource = "price (string)";
            }
        }
        
        // Strategy 2: Legacy and alternative price fields
        String[] priceFields = {"price_str", "current_price", "sale_price", "offer_price", "cost", "amount", "value"};
        
        for (String field : priceFields) {
            if (price == 0.0 && item.has(field)) {
                Object fieldValue = item.opt(field);
                if (fieldValue instanceof String) {
                    price = parsePrice((String) fieldValue);
                    priceSource = field;
                } else if (fieldValue instanceof Number) {
                    price = ((Number) fieldValue).doubleValue();
                    priceSource = field + " (number)";
                }
                
                if (price > 0.0) {
                    logger.debug("Price extraction: Found price {} in field '{}'", price, field);
                    break;
                }
            }
        }
        
        // Strategy 3: Check for nested price structures
        if (price == 0.0) {
            // Check merchant object for price
            JSONObject merchant = item.optJSONObject("merchant");
            if (merchant != null && merchant.has("price")) {
                Object merchantPrice = merchant.opt("price");
                if (merchantPrice instanceof String) {
                    price = parsePrice((String) merchantPrice);
                    priceSource = "merchant.price";
                } else if (merchantPrice instanceof Number) {
                    price = ((Number) merchantPrice).doubleValue();
                    priceSource = "merchant.price (number)";
                }
            }
            
            // Check offer object for price
            JSONObject offer = item.optJSONObject("offer");
            if (offer != null && price == 0.0) {
                for (String field : priceFields) {
                    if (offer.has(field)) {
                        Object offerPrice = offer.opt(field);
                        if (offerPrice instanceof String) {
                            price = parsePrice((String) offerPrice);
                            priceSource = "offer." + field;
                        } else if (offerPrice instanceof Number) {
                            price = ((Number) offerPrice).doubleValue();
                            priceSource = "offer." + field + " (number)";
                        }
                        if (price > 0.0) break;
                    }
                }
            }
        }
        
        // Strategy 4: Extract currency from item if not found
        if (currency == null) {
            currency = JsonUtils.optString(item, "currency", "AED"); // Default to AED for UAE
        }
        
        logger.debug("Price extraction: Final result - Price: {}, Currency: {}, Source: {}", price, currency, priceSource);
        
        return new PriceInfo(price, currency, priceSource);
    }
    
    /**
     * Helper class for price information
     */
    private static class PriceInfo {
        final double value;
        final String currency;
        final String source;
        
        PriceInfo(double value, String currency, String source) {
            this.value = value;
            this.currency = currency;
            this.source = source;
        }
    }
    
    /**
     * Extract specifications from a JSON object by iterating through its properties
     */
    private void extractSpecificationsFromObject(JSONObject obj, List<SpecificationInfo> specifications, String source) {
        if (obj == null) return;
        
        logger.trace("Extracting specs from object (source: {}) with keys: {}", source, obj.keySet());
        
        for (String key : obj.keySet()) {
            Object value = obj.opt(key);
            
            if (value == null) continue;
            
            String stringValue;
            if (value instanceof String) {
                stringValue = (String) value;
            } else if (value instanceof Number) {
                stringValue = value.toString();
            } else if (value instanceof Boolean) {
                stringValue = value.toString();
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < array.length(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(array.opt(i).toString());
                }
                stringValue = sb.toString();
            } else {
                continue; // Skip complex objects
            }
            
            if (!stringValue.trim().isEmpty() && stringValue.length() < 500) {
                String cleanKey = normalizeSpecificationKey(key);
                specifications.add(new SpecificationInfo(cleanKey, stringValue.trim()));
                logger.trace("Extracted spec from object: {} = {}", cleanKey, stringValue);
            }
        }
    }
      /**
     * Extract common product attributes that might be scattered across different fields
     * Enhanced with better error handling for different data types
     */
    private void extractCommonProductAttributes(JSONObject item, List<SpecificationInfo> specifications) {
        // Common attribute mappings with expected data types
        String[][] commonAttributes = {
            {"brand", "Brand"},
            {"model", "Model"},
            {"color", "Color"},
            {"size", "Size"},
            {"weight", "Weight"},
            {"dimensions", "Dimensions"},
            {"material", "Material"},
            {"warranty", "Warranty"},
            {"country_of_origin", "Country of Origin"},
            {"manufacturer", "Manufacturer"},
            {"part_number", "Part Number"},
            {"sku", "SKU"},
            {"upc", "UPC"},
            {"ean", "EAN"},
            {"asin", "ASIN"}
        };
          for (String[] mapping : commonAttributes) {
            String fieldName = mapping[0];
            String displayName = mapping[1];
            
            try {
                String value = JsonUtils.optString(item, fieldName, null);
                if (value != null && !value.trim().isEmpty()) {
                    specifications.add(new SpecificationInfo(displayName, value.trim()));
                    logger.trace("Extracted common attribute: {} = {}", displayName, value);
                }
            } catch (Exception e) {
                logger.debug("Failed to extract attribute '{}': {}", fieldName, e.getMessage());
                // Continue with next attribute
            }
        }
        
        // Handle special numeric attributes that might come as numbers
        handleNumericAttribute(item, "rating", "Rating", specifications);
        handleNumericAttribute(item, "reviews_count", "Reviews Count", specifications);
        handleNumericAttribute(item, "price", "Price", specifications);
        
        // Handle array-based attributes
        handleArrayAttribute(item, "categories", "Categories", specifications);
        handleArrayAttribute(item, "tags", "Tags", specifications);
    }
    
    /**
     * Safely handle numeric attributes that might come as numbers instead of strings
     */
    private void handleNumericAttribute(JSONObject item, String fieldName, String displayName, List<SpecificationInfo> specifications) {
        try {
            if (item.has(fieldName) && !item.isNull(fieldName)) {
                Object value = item.get(fieldName);
                String stringValue;
                
                if (value instanceof Number) {
                    // Format numbers appropriately
                    if (fieldName.equals("rating")) {
                        stringValue = String.format("%.1f", ((Number) value).doubleValue());
                    } else if (fieldName.equals("reviews_count")) {
                        stringValue = String.valueOf(((Number) value).intValue());
                    } else {
                        stringValue = value.toString();
                    }
                } else {
                    stringValue = value.toString();
                }
                
                if (!stringValue.trim().isEmpty()) {
                    specifications.add(new SpecificationInfo(displayName, stringValue.trim()));
                    logger.trace("Extracted numeric attribute: {} = {}", displayName, stringValue);
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract numeric attribute '{}': {}", fieldName, e.getMessage());
        }
    }
    
    /**
     * Handle array-based attributes
     */
    private void handleArrayAttribute(JSONObject item, String fieldName, String displayName, List<SpecificationInfo> specifications) {
        try {
            if (item.has(fieldName) && !item.isNull(fieldName)) {
                Object value = item.get(fieldName);
                if (value instanceof JSONArray) {
                    JSONArray array = (JSONArray) value;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < array.length(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(array.opt(i).toString());
                    }
                    String stringValue = sb.toString();
                    if (!stringValue.trim().isEmpty()) {
                        specifications.add(new SpecificationInfo(displayName, stringValue.trim()));
                        logger.trace("Extracted array attribute: {} = {}", displayName, stringValue);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract array attribute '{}': {}", fieldName, e.getMessage());
        }
    }
    
    /**
     * Clean up specification key names for better readability
     */
    private String normalizeSpecificationKey(String key) {
        if (key == null) return "Unknown";
        
        // Convert snake_case and camelCase to Title Case
        String cleaned = key.replaceAll("_", " ")
                           .replaceAll("([a-z])([A-Z])", "$1 $2");
        
        // Capitalize first letter of each word
        String[] words = cleaned.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                if (result.length() > 0) result.append(" ");
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Remove duplicate specifications based on name similarity
     */
    private List<SpecificationInfo> removeSpecificationDuplicates(List<SpecificationInfo> specifications) {
        List<SpecificationInfo> unique = new ArrayList<>();
        
        for (SpecificationInfo spec : specifications) {
            boolean isDuplicate = false;
            
            for (SpecificationInfo existing : unique) {
                if (areSpecificationNamesSimilar(spec.getName(), existing.getName())) {
                    isDuplicate = true;
                    // Keep the one with more detailed value
                    if (spec.getValue().length() > existing.getValue().length()) {
                        unique.remove(existing);
                        unique.add(spec);
                    }
                    break;
                }
            }
            
            if (!isDuplicate) {
                unique.add(spec);
            }
        }
        
        logger.trace("Deduplication: {} original specs -> {} unique specs", specifications.size(), unique.size());
        return unique;
    }
    
    /**
     * Check if two specification names are similar enough to be considered duplicates
     */
    private boolean areSpecificationNamesSimilar(String name1, String name2) {
        if (name1 == null || name2 == null) return false;
        
        String clean1 = name1.toLowerCase().replaceAll("[^a-z0-9]", "");
        String clean2 = name2.toLowerCase().replaceAll("[^a-z0-9]", "");
        
        return clean1.equals(clean2);
    }
      /**
     * Enhanced scraping that combines search results with detailed product specifications
     * Uses google_shopping_search for initial results, then google_shopping_product for detailed specs
     */    public List<ShoppingProduct> scrapeShoppingResultsEnhanced(String query, String geoLocation, String username, String password) throws Exception {
        logger.info("=====================================");
        logger.info("ENHANCED OXYLABS SCRAPING INITIATED");
        logger.info("=====================================");
        logger.info("Request Configuration:");
        logger.info("  Query: '{}' ", query);
        logger.info("  Geo Location: '{}'", geoLocation);
        logger.info("  Username: '{}***'", username.substring(0, Math.min(3, username.length())));
        logger.info("  Password: [PROVIDED]");
        logger.info("  Strategy: Parallel Enhanced Search + Detailed Product Info");
        logger.info("=====================================");

        // Step 1: Get initial search results with progress callback
        List<ShoppingProduct> searchResults = scrapeShoppingResults(query, geoLocation, username, password, 
            (progress, message) -> logger.debug("Enhanced scraper step 1 progress: {}% - {}", progress, message));
        logger.info("STEP 1 COMPLETE: Found {} products from shopping search", searchResults.size());        // Step 2: Enhance top products with detailed specifications using parallel processing
        List<ShoppingProduct> enhancedProducts = new ArrayList<>();
        int maxProductsToEnhance = Math.min(10, searchResults.size()); // Limit to top 10 to avoid quota issues
          logger.info("STEP 2 STARTING: Enhancing top {} products with detailed specifications and improved images (BATCH PARALLEL PROCESSING)", maxProductsToEnhance);
        
        if (maxProductsToEnhance > 0) {
            List<ShoppingProduct> productsToEnhance = searchResults.subList(0, maxProductsToEnhance);
            
            // Prioritize products that need image enhancement
            productsToEnhance.sort((p1, p2) -> {
                boolean p1NeedsImage = (p1.getImageUrl() == null || p1.getImageUrl().trim().isEmpty());
                boolean p2NeedsImage = (p2.getImageUrl() == null || p2.getImageUrl().trim().isEmpty());
                
                if (p1NeedsImage && !p2NeedsImage) return -1;
                if (!p1NeedsImage && p2NeedsImage) return 1;
                return 0;
            });
            
            logger.info("Product enhancement priorities:");
            for (int idx = 0; idx < Math.min(5, productsToEnhance.size()); idx++) {
                ShoppingProduct p = productsToEnhance.get(idx);
                boolean needsImage = (p.getImageUrl() == null || p.getImageUrl().trim().isEmpty());
                logger.info("  {}. {} - Image: {} | Specs: {}", 
                    idx + 1,
                    p.getTitle().length() > 40 ? p.getTitle().substring(0, 37) + "..." : p.getTitle(),
                    needsImage ? "NEEDED" : "Present",
                    p.getSpecifications() != null ? p.getSpecifications().size() : 0);
            }
            
            // Process in batches of 4 to avoid overwhelming the API
            int batchSize = 4;
            for (int i = 0; i < productsToEnhance.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, productsToEnhance.size());
                List<ShoppingProduct> batch = productsToEnhance.subList(i, endIndex);
                
                logger.info("Processing batch {}/{} ({} products)", 
                    (i/batchSize) + 1, (productsToEnhance.size() + batchSize - 1) / batchSize, batch.size());
                
                // Create CompletableFuture for each product in the batch
                List<CompletableFuture<ShoppingProduct>> batchFutures = batch.stream()
                    .map(product -> CompletableFuture.supplyAsync(() -> {
                        try {
                            ShoppingProduct enhanced = enhanceProductWithDetailedSpecsWithRetry(product, username, password);
                            int globalIndex = productsToEnhance.indexOf(product) + 1;
                            logger.info("Enhanced product {}/{}: {}", globalIndex, maxProductsToEnhance, 
                                product.getTitle().length() > 50 ? product.getTitle().substring(0, 47) + "..." : product.getTitle());
                            return enhanced;
                        } catch (Exception e) {
                            logger.warn("Failed to enhance product '{}': {}", product.getTitle(), e.getMessage());
                            return product;
                        }
                    }, executorService))
                    .collect(Collectors.toList());
                
                // Wait for batch to complete before starting next batch
                List<ShoppingProduct> batchResults = batchFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
                
                enhancedProducts.addAll(batchResults);
                
                // Small delay between batches to be respectful to the API
                if (i + batchSize < productsToEnhance.size()) {
                    try {
                        Thread.sleep(500); // 0.5 second delay between batches
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // Add remaining products without enhancement
        for (int i = maxProductsToEnhance; i < searchResults.size(); i++) {
            enhancedProducts.add(searchResults.get(i));
        }        logger.info("ENHANCED SCRAPING COMPLETE: {} total products, {} enhanced with detailed specs (OPTIMIZED BATCH PARALLEL)", 
            enhancedProducts.size(), maxProductsToEnhance);
        
        return enhancedProducts;
    }

    /**
     * Cleanup method to properly shutdown the executor service
     */
    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down executor service for OxylabsShoppingScraper");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                logger.warn("Executor service did not terminate gracefully, forced shutdown");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }    /**
     * Enhanced version with retry logic for more reliable processing
     */
    private ShoppingProduct enhanceProductWithDetailedSpecsWithRetry(ShoppingProduct product, String username, String password) {
        int maxRetries = 2;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                return enhanceProductWithDetailedSpecs(product, username, password);
            } catch (Exception e) {
                attempt++;
                logger.debug("Enhancement attempt {} failed for '{}': {}", 
                    attempt, product.getTitle(), e.getMessage());
                
                if (attempt >= maxRetries) {
                    logger.warn("All {} enhancement attempts failed for '{}', using original product", 
                        maxRetries, product.getTitle());
                    return product;
                }
                
                // Short delay before retry
                try {
                    Thread.sleep(200 * attempt); // Progressive delay: 200ms, 400ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return product;
                }
            }
        }
        
        return product;
    }

    /**
     * Enhance a single product with detailed specifications using google_shopping_product
     * NEW STRATEGY: Find associated Google Shopping URL for retailer products
     */
    private ShoppingProduct enhanceProductWithDetailedSpecs(ShoppingProduct product, String username, String password) throws Exception {
        if (product.getProductLink() == null || product.getProductLink().trim().isEmpty()) {
            logger.debug("Skipping enhancement for product without URL: {}", product.getTitle());
            return product;
        }

        String productUrl = product.getProductLink();
        logger.debug("Enhancing product: {}", product.getTitle());
        logger.debug("Original URL: {}", productUrl);

        try {
            String googleShoppingUrl = null;
            
            // Check if URL is already a Google Shopping URL
            if (isGoogleShoppingUrl(productUrl)) {
                googleShoppingUrl = productUrl;
                logger.debug("Using direct Google Shopping URL");
            } else {
                // Find associated Google Shopping URL for this retailer product
                googleShoppingUrl = findGoogleShoppingUrl(product, username, password);
            }
            
            if (googleShoppingUrl != null) {
                return fetchDetailedSpecsFromGoogleShopping(product, googleShoppingUrl, username, password);
            } else {
                logger.debug("No Google Shopping URL found, using original product data");
                return product;
            }
            
        } catch (Exception e) {
            logger.warn("Failed to fetch detailed specs for {}: {}", productUrl, e.getMessage());
            return product;
        }
    }
    
    /**
     * Check if URL is a Google Shopping URL that can be used directly
     */
    private boolean isGoogleShoppingUrl(String url) {
        return url != null && (
            url.contains("shopping.google.") || 
            url.contains("google.") && url.contains("/shopping/") ||
            url.contains("google.") && url.contains("product_id=")
        );
    }
    
    /**
     * Find the associated Google Shopping URL for a retailer product
     * Uses targeted search to find the Google Shopping page for this specific product
     */
    private String findGoogleShoppingUrl(ShoppingProduct product, String username, String password) throws Exception {
        logger.debug("Searching for Google Shopping URL for: {}", product.getTitle());
        
        // Create a targeted search query to find this specific product
        String searchQuery = buildTargetedSearchQuery(product);
        logger.debug("Targeted search query: {}", searchQuery);
        
        try {
            // Search specifically within Google Shopping for this product
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("source", "google_shopping_search");
            jsonObject.put("query", searchQuery);
            jsonObject.put("parse", true);
            
            // Add context for targeted search
            JSONArray context = new JSONArray();
            JSONObject resultsLanguage = new JSONObject();
            resultsLanguage.put("key", "results_language");
            resultsLanguage.put("value", "en");
            context.put(resultsLanguage);
            
            JSONObject geoLocation = new JSONObject();
            geoLocation.put("key", "geo_location");
            geoLocation.put("value", "United Arab Emirates");
            context.put(geoLocation);
              jsonObject.put("context", context);
            
            String response = sendOxylabsRequestFast(jsonObject, username, password);
            JSONObject jsonResponse = new JSONObject(response);
            
            // Find matching product in Google Shopping results
            String googleShoppingUrl = extractGoogleShoppingUrlFromSearchResults(jsonResponse, product);
            
            if (googleShoppingUrl != null) {
                logger.debug("Found Google Shopping URL: {}", googleShoppingUrl);
                return googleShoppingUrl;
            }
            
        } catch (Exception e) {
            logger.debug("Failed to find Google Shopping URL: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Build a targeted search query to find the specific product in Google Shopping
     */
    private String buildTargetedSearchQuery(ShoppingProduct product) {
        StringBuilder query = new StringBuilder();
        
        // Extract key terms from product title
        String title = product.getTitle();
        if (title != null) {
            // Clean and extract brand and model
            String[] words = title.split("\\s+");
            int wordsAdded = 0;
            
            for (String word : words) {
                if (wordsAdded >= 6) break; // Limit to 6 key words
                
                word = word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (word.length() >= 3 && !isCommonWord(word)) {
                    if (query.length() > 0) query.append(" ");
                    query.append(word);
                    wordsAdded++;
                }
            }
        }
          // Add price if available to help with matching
        if (product.getPrice() > 0) {
            query.append(" ").append((int) product.getPrice()).append(" AED");
        }
        
        return query.toString();
    }
    
    /**
     * Check if word is a common word that should be excluded from search
     */
    private boolean isCommonWord(String word) {
        String[] commonWords = {"with", "and", "for", "the", "from", "inch", "laptop", "computer", "phone", "black", "white", "silver", "blue", "red"};
        for (String common : commonWords) {
            if (word.equals(common)) return true;
        }
        return false;
    }
    
    /**
     * Extract Google Shopping URL from search results by finding best matching product
     */
    private String extractGoogleShoppingUrlFromSearchResults(JSONObject response, ShoppingProduct targetProduct) {
        try {
            if (!response.has("results") || response.getJSONArray("results").length() == 0) {
                return null;
            }
            
            JSONObject result = response.getJSONArray("results").getJSONObject(0);
            if (!result.has("content") || !result.getJSONObject("content").has("results")) {
                return null;
            }
            
            JSONObject content = result.getJSONObject("content").getJSONObject("results");
            
            // Look for organic results first (most detailed)
            if (content.has("organic") && content.getJSONArray("organic").length() > 0) {
                return findBestMatchingProduct(content.getJSONArray("organic"), targetProduct);
            }
            
            // Fallback to PLA results
            if (content.has("pla") && content.getJSONArray("pla").length() > 0) {
                return findBestMatchingProduct(content.getJSONArray("pla"), targetProduct);
            }
            
        } catch (Exception e) {
            logger.debug("Error extracting Google Shopping URL: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Find the best matching product from search results based on title similarity and price
     */
    private String findBestMatchingProduct(JSONArray products, ShoppingProduct targetProduct) {
        String bestUrl = null;
        double bestScore = 0.0;
        
        for (int i = 0; i < products.length(); i++) {
            try {
                JSONObject product = products.getJSONObject(i);
                
                // Calculate similarity score
                double score = calculateProductSimilarity(product, targetProduct);
                
                if (score > bestScore && score > 0.7) { // Only consider good matches
                    bestScore = score;
                    
                    // Try to extract Google Shopping URL from this product
                    String productUrl = extractProductUrl(product);
                    if (productUrl != null && isGoogleShoppingUrl(productUrl)) {
                        bestUrl = productUrl;
                    }
                }
                
            } catch (Exception e) {
                logger.debug("Error processing product in search results: {}", e.getMessage());
            }
        }
        
        return bestUrl;
    }
    
    /**
     * Calculate similarity between search result and target product
     */
    private double calculateProductSimilarity(JSONObject searchProduct, ShoppingProduct targetProduct) {
        double titleScore = 0.0;
        double priceScore = 0.0;
        
        try {
            // Title similarity
            if (searchProduct.has("title") && targetProduct.getTitle() != null) {
                String searchTitle = searchProduct.getString("title").toLowerCase();
                String targetTitle = targetProduct.getTitle().toLowerCase();
                titleScore = calculateStringSimilarity(searchTitle, targetTitle);
            }
              // Price similarity
            if (searchProduct.has("price") && targetProduct.getPrice() > 0) {
                double searchPrice = extractPriceFromObject(searchProduct);
                double targetPrice = targetProduct.getPrice();
                
                if (searchPrice > 0 && targetPrice > 0) {
                    double priceDiff = Math.abs(searchPrice - targetPrice) / Math.max(searchPrice, targetPrice);
                    priceScore = Math.max(0, 1.0 - priceDiff);
                }
            }
            
        } catch (Exception e) {
            logger.debug("Error calculating similarity: {}", e.getMessage());
        }
        
        // Weight title more heavily than price
        return (titleScore * 0.8) + (priceScore * 0.2);
    }
    
    /**
     * Calculate string similarity using simple word overlap
     */
    private double calculateStringSimilarity(String str1, String str2) {
        String[] words1 = str1.replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        String[] words2 = str2.replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        
        int matches = 0;
        int totalWords = Math.max(words1.length, words2.length);
        
        for (String word1 : words1) {
            if (word1.length() >= 3) {
                for (String word2 : words2) {
                    if (word1.equals(word2)) {
                        matches++;
                        break;
                    }
                }
            }
        }
        
        return totalWords > 0 ? (double) matches / totalWords : 0.0;
    }
    
    /**
     * Extract product URL from search result object
     */
    private String extractProductUrl(JSONObject product) {
        try {
            if (product.has("url")) {
                return product.getString("url");
            }
            if (product.has("product_id")) {
                // Construct Google Shopping URL from product ID
                String productId = product.getString("product_id");
                return "https://shopping.google.com/product/" + productId;
            }
        } catch (Exception e) {
            logger.debug("Error extracting URL: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Fetch detailed specifications from Google Shopping URL
     */
    private ShoppingProduct fetchDetailedSpecsFromGoogleShopping(ShoppingProduct product, String googleShoppingUrl, String username, String password) throws Exception {
        logger.debug("Fetching detailed specs from Google Shopping: {}", googleShoppingUrl);
        
        try {
            // Build request for detailed product info
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("source", "google_shopping_product");
            jsonObject.put("url", googleShoppingUrl);
            jsonObject.put("parse", true);

            JSONArray context = new JSONArray();
            JSONObject resultsLanguage = new JSONObject();
            resultsLanguage.put("key", "results_language");
            resultsLanguage.put("value", "en");
            context.put(resultsLanguage);
            jsonObject.put("context", context);            // Send request
            String response = sendOxylabsRequestFast(jsonObject, username, password);
            
            // Parse detailed product info
            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.has("results") && jsonResponse.getJSONArray("results").length() > 0) {
                JSONObject result = jsonResponse.getJSONArray("results").getJSONObject(0);
                if (result.has("content") && result.getJSONObject("content").has("results")) {
                    JSONObject productData = result.getJSONObject("content").getJSONObject("results");
                    
                    // Extract enhanced specifications
                    List<SpecificationInfo> detailedSpecs = extractDetailedProductSpecifications(productData);
                    
                    // Merge with existing specifications
                    List<SpecificationInfo> mergedSpecs = mergeSpecifications(product.getSpecifications(), detailedSpecs);
                    product.setSpecifications(mergedSpecs);
                    
                    // Extract additional details if available
                    enhanceProductWithAdditionalDetails(product, productData);
                    
                    logger.debug("Enhanced product with {} detailed specifications from Google Shopping", 
                        detailedSpecs.size());
                    return product;
                }
            }
            
        } catch (Exception e) {
            logger.warn("Failed to fetch specs from Google Shopping {}: {}", googleShoppingUrl, e.getMessage());
        }
        
        return product;
    }
    
    /**
     * Extract price from JSON object with multiple fallback strategies
     */
    private double extractPriceFromObject(JSONObject obj) {
        try {
            if (obj.has("price")) {
                Object priceObj = obj.get("price");
                if (priceObj instanceof Number) {
                    return ((Number) priceObj).doubleValue();
                } else if (priceObj instanceof String) {
                    return parsePrice((String) priceObj);
                }
            }
            
            if (obj.has("price_str")) {
                return parsePrice(obj.getString("price_str"));
            }
              } catch (Exception e) {
            logger.debug("Error extracting price: {}", e.getMessage());
        }
        
        return 0.0;
    }

    /**
     * Extract detailed specifications from google_shopping_product response
     */
    private List<SpecificationInfo> extractDetailedProductSpecifications(JSONObject productData) {
        List<SpecificationInfo> specs = new ArrayList<>();
        
        try {
            // Extract from specifications array (Oxylabs standard format)
            if (productData.has("specifications") && !productData.isNull("specifications")) {
                JSONArray specificationsArray = productData.getJSONArray("specifications");
                logger.debug("Found detailed specifications array with {} sections", specificationsArray.length());
                
                for (int i = 0; i < specificationsArray.length(); i++) {
                    JSONObject specSection = specificationsArray.getJSONObject(i);
                    String sectionTitle = JsonUtils.optString(specSection, "section_title", "General");
                    
                    if (specSection.has("items") && !specSection.isNull("items")) {
                        JSONArray items = specSection.getJSONArray("items");
                        logger.debug("Processing specification section '{}' with {} items", sectionTitle, items.length());
                        
                        for (int j = 0; j < items.length(); j++) {
                            JSONObject item = items.getJSONObject(j);
                            String title = JsonUtils.optString(item, "title", null);
                            String value = JsonUtils.optString(item, "value", null);
                            
                            if (title != null && value != null && !title.trim().isEmpty() && !value.trim().isEmpty()) {
                                // Enhance title with section context
                                String enhancedTitle = enhanceSpecificationTitle(title, sectionTitle);
                                specs.add(new SpecificationInfo(enhancedTitle, value.trim()));
                                logger.trace("Detailed spec: {} = {} (section: {})", enhancedTitle, value, sectionTitle);
                            }
                        }
                    }
                }
            }
            
            // Extract from reviews and ratings
            extractReviewsAndRatings(productData, specs);
            
            // Extract from product highlights
            extractProductHighlights(productData, specs);
            
            // Extract from description and features
            extractDescriptionFeatures(productData, specs);
            
        } catch (Exception e) {
            logger.warn("Error extracting detailed specifications: {}", e.getMessage());
        }
        
        return specs;
    }

    /**
     * Send request to Oxylabs API (extracted for reuse)
     */
    private String sendOxylabsRequest(JSONObject jsonObject, String username, String password) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String credential = Credentials.basic(username, password);
                    Request originalRequest = chain.request();
                    Request newRequest = originalRequest.newBuilder()
                            .header("Authorization", credential)
                            .build();
                    return chain.proceed(newRequest);
                })
                .readTimeout(180, TimeUnit.SECONDS)
                .build();

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonObject.toString(), mediaType);

        Request request = new Request.Builder()
                .url("https://realtime.oxylabs.io/v1/queries")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }
            return response.body().string();
        }
    }    /**
     * Faster version of sendOxylabsRequest with optimized timeouts for parallel processing
     */
    private String sendOxylabsRequestFast(JSONObject jsonObject, String username, String password) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String credential = Credentials.basic(username, password);
                    Request originalRequest = chain.request();
                    Request newRequest = originalRequest.newBuilder()
                            .header("Authorization", credential)
                            .build();
                    return chain.proceed(newRequest);
                })
                .connectTimeout(5, TimeUnit.SECONDS)     // Reduced from 10 seconds for faster connection
                .readTimeout(20, TimeUnit.SECONDS)       // Reduced from 30 seconds for faster response
                .writeTimeout(5, TimeUnit.SECONDS)       // Reduced write timeout
                .build();

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonObject.toString(), mediaType);

        Request request = new Request.Builder()
                .url("https://realtime.oxylabs.io/v1/queries")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }
            return response.body().string();
        }
    }

    /**
     * Merge specifications from search results with detailed specifications
     */
    private List<SpecificationInfo> mergeSpecifications(List<SpecificationInfo> searchSpecs, List<SpecificationInfo> detailedSpecs) {
        List<SpecificationInfo> merged = new ArrayList<>();
        
        // Add detailed specs first (they're more comprehensive)
        if (detailedSpecs != null) {
            merged.addAll(detailedSpecs);
        }
        
        // Add search specs that don't duplicate detailed specs
        if (searchSpecs != null) {
            for (SpecificationInfo searchSpec : searchSpecs) {
                boolean isDuplicate = merged.stream()
                    .anyMatch(detailedSpec -> areSpecificationNamesSimilar(searchSpec.getName(), detailedSpec.getName()));
                
                if (!isDuplicate) {
                    merged.add(searchSpec);
                }
            }
        }
        
        // Remove duplicates and return
        return removeSpecificationDuplicates(merged);
    }    /**
     * Enhance product with additional details from detailed product response
     * Enhanced to prioritize better image extraction
     */
    private void enhanceProductWithAdditionalDetails(ShoppingProduct product, JSONObject productData) {
        try {
            // Enhanced description
            String detailedDescription = JsonUtils.optString(productData, "description", null);
            if (detailedDescription != null && !detailedDescription.trim().isEmpty()) {
                if (product.getDescription() == null || product.getDescription().length() < detailedDescription.length()) {
                    product.setDescription(detailedDescription);
                }
            }
            
            // Enhanced image URL - prioritize higher quality images from detailed response
            String currentImageUrl = product.getImageUrl();
            String detailedImageUrl = extractBestImageUrl(productData);
            
            if (detailedImageUrl != null && !detailedImageUrl.trim().isEmpty()) {
                // Always update if we don't have an image, or if the detailed image looks higher quality
                if (currentImageUrl == null || currentImageUrl.trim().isEmpty()) {
                    product.setImageUrl(detailedImageUrl);
                    logger.debug("Enhanced product with image URL (no previous): {}", 
                        detailedImageUrl.length() > 50 ? detailedImageUrl.substring(0, 47) + "..." : detailedImageUrl);
                } else {
                    // Compare image quality - prefer larger images and those from main product sources
                    int currentQuality = estimateImageQuality(currentImageUrl);
                    int detailedQuality = estimateImageQuality(detailedImageUrl);
                    
                    // Prefer detailed images if they're significantly better or from better sources
                    if (detailedQuality > currentQuality * 1.5 || 
                        detailedImageUrl.contains("main") || 
                        detailedImageUrl.contains("primary") ||
                        detailedImageUrl.contains("large") ||
                        detailedImageUrl.contains("1200") ||
                        detailedImageUrl.contains("1500")) {
                        product.setImageUrl(detailedImageUrl);
                        logger.debug("Enhanced product with better quality image URL (quality: {} vs {}): {}", 
                            detailedQuality, currentQuality,
                            detailedImageUrl.length() > 50 ? detailedImageUrl.substring(0, 47) + "..." : detailedImageUrl);
                    }
                }
            }
            
            // Additional product attributes
            enhanceProductAttributes(product, productData);
            
        } catch (Exception e) {
            logger.debug("Error enhancing product details: {}", e.getMessage());
        }
    }

    /**
     * Extract reviews and ratings information
     */
    private void extractReviewsAndRatings(JSONObject productData, List<SpecificationInfo> specs) {
        try {
            // Extract rating
            if (productData.has("rating") && !productData.isNull("rating")) {
                Object rating = productData.get("rating");
                if (rating instanceof Number) {
                    specs.add(new SpecificationInfo("Customer Rating", String.format("%.1f", ((Number) rating).doubleValue())));
                } else {
                    specs.add(new SpecificationInfo("Customer Rating", rating.toString()));
                }
            }
            
            // Extract review count
            if (productData.has("reviews_count") && !productData.isNull("reviews_count")) {
                Object reviewCount = productData.get("reviews_count");
                specs.add(new SpecificationInfo("Total Reviews", reviewCount.toString()));
            }
            
            // Extract reviews array for additional insights
            if (productData.has("reviews") && !productData.isNull("reviews")) {
                JSONArray reviews = productData.getJSONArray("reviews");
                if (reviews.length() > 0) {
                    specs.add(new SpecificationInfo("Review Availability", "Available (" + reviews.length() + " reviews)"));
                    
                    // Extract common themes from reviews (first few)
                    extractReviewThemes(reviews, specs);
                }
            }
            
        } catch (Exception e) {
            logger.debug("Error extracting reviews and ratings: {}", e.getMessage());
        }
    }

    /**
     * Extract product highlights and key features
     */
    private void extractProductHighlights(JSONObject productData, List<SpecificationInfo> specs) {
        try {
            // Extract highlights array
            if (productData.has("highlights") && !productData.isNull("highlights")) {
                JSONArray highlights = productData.getJSONArray("highlights");
                StringBuilder highlightText = new StringBuilder();
                
                for (int i = 0; i < highlights.length() && i < 5; i++) { // Limit to first 5
                    if (highlightText.length() > 0) highlightText.append(" • ");
                    highlightText.append(highlights.getString(i));
                }
                
                if (highlightText.length() > 0) {
                    specs.add(new SpecificationInfo("Key Highlights", highlightText.toString()));
                }
            }
            
            // Extract about section
            if (productData.has("about_this_item") && !productData.isNull("about_this_item")) {
                Object about = productData.get("about_this_item");
                if (about instanceof JSONArray) {
                    JSONArray aboutArray = (JSONArray) about;
                    StringBuilder aboutText = new StringBuilder();
                    
                    for (int i = 0; i < aboutArray.length() && i < 3; i++) { // Limit to first 3
                        if (aboutText.length() > 0) aboutText.append(" • ");
                        aboutText.append(aboutArray.getString(i));
                    }
                    
                    if (aboutText.length() > 0) {
                        specs.add(new SpecificationInfo("Product Features", aboutText.toString()));
                    }
                } else if (about instanceof String) {
                    String aboutStr = (String) about;
                    if (aboutStr.length() < 500) { // Only if not too long
                        specs.add(new SpecificationInfo("Product Features", aboutStr));
                    }
                }
            }
            
        } catch (Exception e) {
            logger.debug("Error extracting product highlights: {}", e.getMessage());
        }
    }

    /**
     * Extract description and features from detailed product data
     */
    private void extractDescriptionFeatures(JSONObject productData, List<SpecificationInfo> specs) {
        try {
            // Extract feature bullets
            if (productData.has("feature_bullets") && !productData.isNull("feature_bullets")) {
                JSONArray features = productData.getJSONArray("feature_bullets");
                StringBuilder featureText = new StringBuilder();
                
                for (int i = 0; i < features.length() && i < 4; i++) { // Limit to first 4
                    if (featureText.length() > 0) featureText.append(" • ");
                    featureText.append(features.getString(i));
                }
                
                if (featureText.length() > 0) {
                    specs.add(new SpecificationInfo("Features", featureText.toString()));
                }
            }
            
            // Extract product dimensions
            if (productData.has("product_dimensions") && !productData.isNull("product_dimensions")) {
                String dimensions = JsonUtils.optString(productData, "product_dimensions", null);
                if (dimensions != null && !dimensions.trim().isEmpty()) {
                    specs.add(new SpecificationInfo("Product Dimensions", dimensions));
                }
            }
            
        } catch (Exception e) {
            logger.debug("Error extracting description features: {}", e.getMessage());
        }
    }

    /**
     * Extract themes from reviews for additional product insights
     */
    private void extractReviewThemes(JSONArray reviews, List<SpecificationInfo> specs) {
        try {
            Map<String, Integer> themes = new HashMap<>();
            int reviewsToAnalyze = Math.min(5, reviews.length());
            
            for (int i = 0; i < reviewsToAnalyze; i++) {
                JSONObject review = reviews.getJSONObject(i);
                String reviewText = JsonUtils.optString(review, "review_text", "").toLowerCase();
                
                // Look for common positive/negative themes
                if (reviewText.contains("battery") || reviewText.contains("power")) {
                    themes.put("battery life", themes.getOrDefault("battery life", 0) + 1);
                }
                if (reviewText.contains("quality") || reviewText.contains("build")) {
                    themes.put("build quality", themes.getOrDefault("build quality", 0) + 1);
                }
                if (reviewText.contains("price") || reviewText.contains("value")) {
                    themes.put("value for money", themes.getOrDefault("value for money", 0) + 1);
                }
                if (reviewText.contains("fast") || reviewText.contains("speed")) {
                    themes.put("performance", themes.getOrDefault("performance", 0) + 1);
                }
            }
            
            // Add significant themes as specifications
            themes.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2) // Mentioned in at least 2 reviews
                .forEach(entry -> specs.add(new SpecificationInfo("Review Theme", 
                    entry.getKey() + " (mentioned in " + entry.getValue() + " reviews)")));
                    
        } catch (Exception e) {
            logger.debug("Error extracting review themes: {}", e.getMessage());
        }
    }

    /**
     * Enhance product attributes with detailed data
     */
    private void enhanceProductAttributes(ShoppingProduct product, JSONObject productData) {
        try {
            // Extract brand if not already set
            if (productData.has("brand") && !productData.isNull("brand")) {
                String brand = JsonUtils.optString(productData, "brand", null);
                if (brand != null && !brand.trim().isEmpty()) {
                    // You can add brand to product if your model supports it
                    logger.trace("Enhanced brand information: {}", brand);
                }
            }
            
            // Extract additional seller information
            if (productData.has("seller") && !productData.isNull("seller")) {
                Object sellerObj = productData.get("seller");
                if (sellerObj instanceof JSONObject) {
                    JSONObject seller = (JSONObject) sellerObj;
                    String sellerName = JsonUtils.optString(seller, "name", null);
                    if (sellerName != null && (product.getSeller() == null || product.getSeller().trim().isEmpty())) {
                        product.setSeller(sellerName);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.debug("Error enhancing product attributes: {}", e.getMessage());
        }
    }
}
