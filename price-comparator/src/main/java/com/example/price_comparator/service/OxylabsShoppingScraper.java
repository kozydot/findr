package com.example.price_comparator.service;

import com.example.price_comparator.dto.ShoppingProduct;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OxylabsShoppingScraper {

    private static final Logger logger = LoggerFactory.getLogger(OxylabsShoppingScraper.class);

    @Value("${oxylabs.username}")
    private String username;

    @Value("${oxylabs.password}")
    private String password;

    public List<ShoppingProduct> scrapeShoppingResults(String query, String geoLocation) {
        List<ShoppingProduct> products = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("source", "google_shopping_search");
            jsonObject.put("geo_location", geoLocation);
            jsonObject.put("query", query);
            jsonObject.put("parse", true);

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

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject responseJson = new JSONObject(responseBody);

                    JSONArray organicResults = responseJson.getJSONArray("results")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONObject("results")
                            .getJSONArray("organic");

                    for (int i = 0; i < organicResults.length(); i++) {
                        JSONObject item = organicResults.getJSONObject(i);
                        ShoppingProduct product = new ShoppingProduct();
                        product.setTitle(item.optString("title", null));
                        
                        // Extract price from price_str and parse it
                        String priceStr = item.optString("price_str", "0.0");
                        try {
                            // This regex finds the first valid decimal number in the string
                            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d[\\d,.]*\\d)");
                            java.util.regex.Matcher matcher = pattern.matcher(priceStr);
                            if (matcher.find()) {
                                String priceValue = matcher.group(1).replaceAll(",", "");
                                product.setPrice(Double.parseDouble(priceValue));
                            } else {
                                product.setPrice(0.0);
                            }
                        } catch (NumberFormatException e) {
                            logger.error("Could not parse price string: {}", item.optString("price_str"), e);
                            product.setPrice(0.0);
                        }

                        // Extract seller from nested merchant object
                        JSONObject merchant = item.optJSONObject("merchant");
                        if (merchant != null) {
                            product.setSeller(merchant.optString("name", null));
                        }

                        product.setProductLink(item.optString("url", null));
                        product.setImageUrl(item.optString("image", null));
                        products.add(product);
                    }
                } else {
                    logger.error("Oxylabs request failed with code: {}", response.code());
                }
            }
        } catch (Exception e) {
            logger.error("Error during Oxylabs scraping: {}", e.getMessage(), e);
        }
        return products;
    }
}