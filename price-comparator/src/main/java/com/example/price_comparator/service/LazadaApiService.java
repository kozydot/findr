package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class LazadaApiService implements RetailerApiService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private static final String API_KEY = "789a3d99e9mshf91eeac2b820feap133038jsnf9a117afcd70";
    private static final String API_HOST = "lazada-datahub.p.rapidapi.com";

    @Override
    public ProductDocument searchProducts(String query) {
        // For now, we'll just use the getProductDetails method with a hardcoded item ID for testing.
        return getProductDetails("4896411369");
    }

    @Override
    public ProductDocument getProductDetails(String id) {
        try {
            Request request = new Request.Builder()
                    .url("https://lazada-datahub.p.rapidapi.com/item_detail?itemId=" + id + "&region=TH&locale=en_US")
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
            System.out.println("Lazada API Response: " + responseBody);
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONObject productJson = jsonResponse.getJSONObject("data").getJSONArray("list").getJSONObject(0);

            ProductDocument product = new ProductDocument();
            product.setId(productJson.getString("itemId"));
            product.setName(productJson.getString("name"));
            product.setPrice(productJson.getString("price"));
            product.setImageUrl(productJson.getString("image"));
            product.setDescription(productJson.getString("name")); // No description field in this response
            product.setRating(productJson.optDouble("ratingScore", 0.0));
            product.setReviews(productJson.optInt("review", 0));

            return product;

        } catch (Exception e) {
            System.err.println("Error getting product details from Lazada API: " + e.getMessage());
            return null;
        }
    }
}
