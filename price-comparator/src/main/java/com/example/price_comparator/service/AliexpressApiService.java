package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AliexpressApiService implements RetailerApiService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private static final String API_KEY = "789a3d99e9mshf91eeac2b820feap133038jsnf9a117afcd70";
    private static final String API_HOST = "aliexpress-datahub.p.rapidapi.com";

    @Override
    public ProductDocument searchProducts(String query) {
        // For now, we'll just use the getProductDetails method with a hardcoded item ID for testing.
        return getProductDetails("1005005244562338");
    }

    @Override
    public ProductDocument getProductDetails(String id) {
        try {
            Request request = new Request.Builder()
                    .url("https://aliexpress-datahub.p.rapidapi.com/item_detail_2?itemId=" + id)
                    .get()
                    .addHeader("x-rapidapi-key", API_KEY)
                    .addHeader("x-rapidapi-host", API_HOST)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                System.err.println("API request failed with code: " + response.code());
                return null;
            }

            String responseBody = response.body().string();
            System.out.println("Aliexpress API Response: " + responseBody);
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONObject productJson = jsonResponse.getJSONObject("result").getJSONObject("item");

            ProductDocument product = new ProductDocument();
            product.setId(String.valueOf(productJson.getLong("itemId")));
            product.setName(productJson.getString("title"));
            product.setPrice(productJson.getJSONObject("sku").getJSONObject("def").getString("promotionPrice"));
            product.setImageUrl(productJson.getJSONArray("images").getString(0));
            product.setDescription(productJson.getString("title")); // No description field in this response
            product.setRating(0.0);
            product.setReviews(0);

            return product;

        } catch (Exception e) {
            System.err.println("Error getting product details from Aliexpress API: " + e.getMessage());
            return null;
        }
    }
}
