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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OxylabsShoppingScraper {

    private static final Logger logger = LoggerFactory.getLogger(OxylabsShoppingScraper.class);    public List<ShoppingProduct> scrapeShoppingResults(String query, String geoLocation, String username, String password, BiConsumer<Integer, String> progressCallback) {
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
            progressCallback.accept(10, "Initiating scraping process...");
            
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
            progressCallback.accept(25, "Sending request to Oxylabs...");
            
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
                    progressCallback.accept(50, "Response received, parsing results...");
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
                        progressCallback.accept(90, "Found " + products.size() + " potential offers.");                        logger.info("EXTRACTION SUMMARY:");
                        logger.info("  Total result objects processed: {}", results.length());
                        logger.info("  Valid products extracted: {}", products.size());
                        double successRate = results.length() > 0 ? (products.size() * 100.0 / results.length()) : 0.0;
                        logger.info("  Extraction success rate: {}%", String.format("%.1f", successRate));
                    } else {
                        logger.warn("No results array found in response");
                        logger.debug("Available response keys: {}", responseJson.keySet());
                        progressCallback.accept(90, "No offers found in the initial response.");
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
                    
                    progressCallback.accept(100, "Failed to retrieve offers - HTTP " + response.code());
                }
            }        } catch (Exception e) {
            logger.error("OXYLABS SCRAPING ERROR OCCURRED");
            logger.error("  Exception Type: {}", e.getClass().getSimpleName());
            logger.error("  Exception Message: {}", e.getMessage());
            logger.error("  Query: '{}'", query);
            logger.error("  Geo Location: '{}'", geoLocation);
            logger.error("  Products collected before error: {}", products.size());
            logger.error("  Full Exception Details:", e);
            progressCallback.accept(100, "Error occurred during scraping: " + e.getClass().getSimpleName());
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
       
        progressCallback.accept(100, "Comparison finished.");
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
                product.setTitle(title);

                // Price extraction with detailed logging
                double price = 0.0;
                String priceSource = "none";
                
                if (item.has("price")) {
                    Object priceObj = item.get("price");
                    if (priceObj instanceof Number) {
                        price = ((Number) priceObj).doubleValue();
                        priceSource = "price (number)";
                    } else if (priceObj instanceof String) {
                        price = parsePrice((String) priceObj);
                        priceSource = "price (string)";
                    }
                }
                
                if (price == 0.0 && item.has("price_str")) {
                    price = parsePrice(item.getString("price_str"));
                    priceSource = "price_str";
                }
                
                product.setPrice(price);
                logger.debug("  Price: {} (source: {})", price, priceSource);

                // Merchant/Seller processing
                String finalUrl = url;
                String sellerName = null;
                String sellerSource = "none";

                JSONObject merchant = item.optJSONObject("merchant");
                if (merchant != null) {
                    sellerName = JsonUtils.optString(merchant, "name", null);
                    String merchantUrl = JsonUtils.optString(merchant, "url", null);
                    if (merchantUrl != null && !merchantUrl.isEmpty()) {
                        finalUrl = merchantUrl;
                    }
                    sellerSource = "merchant object";
                    logger.debug("  Merchant data found: name='{}', url='{}'", sellerName, merchantUrl);
                }
                
                if (sellerName == null) {
                    sellerName = JsonUtils.optString(item, "seller", null);
                    if (sellerName != null) {
                        sellerSource = "seller field";
                    }
                }

                logger.debug("  Seller: '{}' (source: {})", sellerName, sellerSource);
                logger.debug("  Final URL: '{}'", finalUrl);

                // Skip Google links
                if (finalUrl.contains("google.com")) {
                    logger.debug("Skipping product with Google link: {}", finalUrl);
                    skippedItems++;
                    continue;
                }

                product.setSeller(sellerName);
                product.setProductLink(finalUrl);
                product.setImageUrl(JsonUtils.optString(item, "thumbnail", null));
                product.setDescription(JsonUtils.optString(item, "description", null));

                // Specifications processing
                JSONArray specsArray = item.optJSONArray("specifications");
                if (specsArray != null) {
                    List<SpecificationInfo> specifications = new ArrayList<>();
                    for (int j = 0; j < specsArray.length(); j++) {
                        JSONObject specObject = specsArray.optJSONObject(j);
                        if (specObject != null) {
                            String specName = JsonUtils.optString(specObject, "name", null);
                            String specValue = JsonUtils.optString(specObject, "value", null);
                            specifications.add(new SpecificationInfo(specName, specValue));
                        }
                    }
                    product.setSpecifications(specifications);
                    logger.debug("  Specifications: {} items found", specifications.size());
                }                products.add(product);
                validProducts++;
                logger.info("PRODUCT EXTRACTED [{}]: '{}' from {} at {} AED", 
                    validProducts, 
                    title.length() > 50 ? title.substring(0, 47) + "..." : title,
                    sellerName != null ? sellerName : "Unknown Seller",
                    String.format("%.2f", price));
                    
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
        }
    }private double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) {
            logger.trace("Price parsing: Input string is null or empty");
            return 0.0;
        }
        
        logger.trace("Price parsing: Input string = '{}'", priceStr);
        
        try {
            // This regex is designed to find the first valid floating-point number in the string.
            // It handles currency symbols, commas, and other text.
            Pattern pattern = Pattern.compile("[\\d,]*\\.?\\d+");
            Matcher matcher = pattern.matcher(priceStr);
            if (matcher.find()) {
                String priceValue = matcher.group(0).replaceAll(",", "");
                double parsedPrice = Double.parseDouble(priceValue);
                
                logger.trace("Price parsing: Successfully extracted {} from '{}'", parsedPrice, priceStr);
                return parsedPrice;
            } else {
                logger.debug("Price parsing: No numeric pattern found in '{}'", priceStr);
            }
        } catch (NumberFormatException e) {
            logger.warn("Price parsing: NumberFormatException while parsing '{}': {}", priceStr, e.getMessage());
        } catch (Exception e) {
            logger.error("Price parsing: Unexpected error while parsing '{}': {}", priceStr, e.getMessage(), e);
        }
        
        logger.debug("Price parsing: Could not extract valid price from '{}'", priceStr);
        return 0.0;
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
    }
}
