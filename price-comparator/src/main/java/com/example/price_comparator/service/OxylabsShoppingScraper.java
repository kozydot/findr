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

    private static final Logger logger = LoggerFactory.getLogger(OxylabsShoppingScraper.class);

    public List<ShoppingProduct> scrapeShoppingResults(String query, String geoLocation, String username, String password, BiConsumer<Integer, String> progressCallback) {
        List<ShoppingProduct> products = new ArrayList<>();
        try {
            progressCallback.accept(10, "Initiating scraping process...");
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

           progressCallback.accept(25, "Sending request to Oxylabs...");
           try (Response response = client.newCall(request).execute()) {
               if (response.isSuccessful() && response.body() != null) {
                   progressCallback.accept(50, "Response received, parsing results...");
                   String responseBody = response.body().string();
                   JSONObject responseJson = new JSONObject(responseBody);
                   logger.debug("Full Oxylabs Response: {}", responseBody);

                   JSONArray results = responseJson.optJSONArray("results");
                   if (results != null && results.length() > 0) {
                       extractProducts(results, products);
                       progressCallback.accept(90, "Found " + products.size() + " potential offers.");
                   } else {
                       logger.info("No results array found for query: {}", query);
                       progressCallback.accept(90, "No offers found in the initial response.");
                   }

               } else {
                   logger.error("Oxylabs request failed with code: {}", response.code());
                   progressCallback.accept(100, "Failed to retrieve offers.");
               }
           }
        } catch (Exception e) {
            logger.error("Error during Oxylabs scraping: {}", e.getMessage(), e);
        }
        
       if (products.isEmpty()) {
           logger.warn("No shopping offers found for query: {}", query);
       }
       
       progressCallback.accept(100, "Comparison finished.");
       return products;
   }

    private void extractProducts(JSONArray jsonArray, List<ShoppingProduct> products) {
        if (jsonArray == null) {
            return;
        }
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.optJSONObject(i);
            if (item == null) {
                continue;
            }

            // Check if this object is a product
            String title = JsonUtils.optString(item, "title", null);
            String url = JsonUtils.optString(item, "url", null);

            if (title != null && url != null && !url.isEmpty()) {
                ShoppingProduct product = new ShoppingProduct();
                product.setTitle(title);

                double price = 0.0;
                if (item.has("price")) {
                    Object priceObj = item.get("price");
                    if (priceObj instanceof Number) {
                        price = ((Number) priceObj).doubleValue();
                    } else if (priceObj instanceof String) {
                        price = parsePrice((String) priceObj);
                    }
                }
                
                if (price == 0.0 && item.has("price_str")) {
                    price = parsePrice(item.getString("price_str"));
                }
                product.setPrice(price);

                String finalUrl = url;
                String sellerName = null;

                JSONObject merchant = item.optJSONObject("merchant");
                if (merchant != null) {
                    sellerName = JsonUtils.optString(merchant, "name", null);
                    String merchantUrl = JsonUtils.optString(merchant, "url", null);
                    if (merchantUrl != null && !merchantUrl.isEmpty()) {
                        finalUrl = merchantUrl;
                    }
                }
                
                if (sellerName == null) {
                    sellerName = JsonUtils.optString(item, "seller", null);
                }

                if (finalUrl.contains("google.com")) {
                    logger.warn("Skipping product with Google link: {}", finalUrl);
                    continue;
                }

                product.setSeller(sellerName);
                product.setProductLink(finalUrl);
                product.setImageUrl(JsonUtils.optString(item, "thumbnail", null));

                product.setDescription(JsonUtils.optString(item, "description", null));

                JSONArray specsArray = item.optJSONArray("specifications");
                if (specsArray != null) {
                    List<SpecificationInfo> specifications = new ArrayList<>();
                    for (int j = 0; j < specsArray.length(); j++) {
                        JSONObject specObject = specsArray.optJSONObject(j);
                        if (specObject != null) {
                            specifications.add(new SpecificationInfo(
                                JsonUtils.optString(specObject, "name", null),
                                JsonUtils.optString(specObject, "value", null)
                            ));
                        }
                    }
                    product.setSpecifications(specifications);
                }

                products.add(product);
                logger.info("Successfully added product '{}' with link {}", product.getTitle(), finalUrl);
            } else {
                // If not a product, check for nested arrays to parse
                logger.warn("Skipping item due to missing title or url. Item JSON: {}", item.toString(2));
                if (item.has("items")) {
                    extractProducts(item.optJSONArray("items"), products);
                }
                if (item.has("pla")) {
                    extractProducts(item.optJSONArray("pla"), products);
                }
                if (item.has("organic")) {
                    extractProducts(item.optJSONArray("organic"), products);
                }
                if (item.has("content")) {
                    JSONObject content = item.optJSONObject("content");
                    if (content != null && content.has("results")) {
                        JSONObject results = content.optJSONObject("results");
                        if (results != null) {
                            if (results.has("pla")) {
                                extractProducts(results.optJSONArray("pla"), products);
                            }
                            if (results.has("organic")) {
                                extractProducts(results.optJSONArray("organic"), products);
                            }
                        }
                    }
                }
            }
        }
    }

    private double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) {
            return 0.0;
        }
        try {
            // This regex is designed to find the first valid floating-point number in the string.
            // It handles currency symbols, commas, and other text.
            Pattern pattern = Pattern.compile("[\\d,]*\\.?\\d+");
            Matcher matcher = pattern.matcher(priceStr);
            if (matcher.find()) {
                String priceValue = matcher.group(0).replaceAll(",", "");
                return Double.parseDouble(priceValue);
            }
        } catch (NumberFormatException e) {
            logger.error("Could not parse price string: '{}'", priceStr, e);
        }
        logger.warn("Could not extract a valid price from string: '{}'", priceStr);
        return 0.0;
    }
}
