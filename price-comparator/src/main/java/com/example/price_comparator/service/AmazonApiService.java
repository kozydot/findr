package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class AmazonApiService implements RetailerApiService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private static final String API_KEY = "789a3d99e9mshf91eeac2b820feap133038jsnf9a117afcd70";
    private static final String API_HOST = "real-time-amazon-data.p.rapidapi.com";

    public ProductDocument searchProducts(String query) {
        // For now, we'll just use the getProductDetails method with a hardcoded ASIN for testing.
        return getProductDetails("B07ZPKBL9V");
    }

    public ProductDocument getProductDetails(String asin) {
        try {
            String url = "https://" + API_HOST + "/product-details?asin=" + asin + "&country=US";
            Request request = new Request.Builder()
                    .url(url)
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
            JSONObject jsonResponse = new JSONObject(responseBody);

            if (!jsonResponse.getString("status").equals("OK")) {
                System.err.println("API returned an error: " + jsonResponse.getJSONObject("error").getString("message"));
                return null;
            }

            JSONObject productJson = jsonResponse.getJSONObject("data");
            ProductDocument product = new ProductDocument();
            product.setId(productJson.optString("asin", null));
            product.setName(productJson.getString("product_title"));
            product.setPrice(productJson.optString("product_price", null));
            product.setOriginalPrice(productJson.optString("product_original_price", null));
            product.setCurrency(productJson.getString("currency"));
            product.setProductUrl(productJson.getString("product_url"));
            product.setAvailability(productJson.optString("product_availability", null));
            product.setAbout(productJson.getJSONArray("about_product").toList().stream().map(Object::toString).collect(java.util.stream.Collectors.toList()));
            product.setProductInformation(productJson.getJSONObject("product_information").toMap().entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
            product.setPhotos(productJson.getJSONArray("product_photos").toList().stream().map(Object::toString).collect(java.util.stream.Collectors.toList()));
            product.setImageUrl(productJson.getString("product_photo"));
            product.setDescription(productJson.optString("product_description", null));
            product.setRating(Double.parseDouble(productJson.getString("product_star_rating")));
            product.setReviews(productJson.getInt("product_num_ratings"));

            return product;

        } catch (IOException e) {
            System.err.println("Error getting product details: " + e.getMessage());
            return null;
        }
    }
}
