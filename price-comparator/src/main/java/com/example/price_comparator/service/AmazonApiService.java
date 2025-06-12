package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AmazonApiService implements RetailerApiService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private static final String API_KEY = "a545c6d02bmshbc13ceb1ab5e1e6p1c6fabjsnf07da51d1585";
    private static final String API_HOST = "real-time-amazon-data.p.rapidapi.com";

    public List<ProductDocument> searchProducts(String query) {
        try {
            String url = "https://" + API_HOST + "/best-sellers?category=" + query + "&type=BEST_SELLERS&page=1&country=AE&language=en_AE";
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("x-rapidapi-key", API_KEY)
                    .addHeader("x-rapidapi-host", API_HOST)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                System.err.println("API request failed with code: " + response.code());
                return new ArrayList<>();
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);

            if (!jsonResponse.getString("status").equals("OK")) {
                System.err.println("API returned an error: " + jsonResponse.toString());
                return new ArrayList<>();
            }

            List<ProductDocument> products = new ArrayList<>();
            JSONArray productsJson = jsonResponse.getJSONObject("data").getJSONArray("best_sellers");
            for (int i = 0; i < productsJson.length(); i++) {
                JSONObject productJson = productsJson.getJSONObject(i);
                ProductDocument product = new ProductDocument();
                product.setId(productJson.optString("asin", null));
                product.setName(productJson.optString("product_title", null));
                product.setPrice(productJson.optString("product_price", null));
                product.setOriginalPrice(null); // Not available in this response
                product.setCurrency(jsonResponse.getJSONObject("parameters").optString("country", null));
                product.setProductUrl(productJson.optString("product_url", null));
                product.setAvailability(null); // Not available in this response
                product.setImageUrl(productJson.optString("product_photo", null));
                product.setRating(productJson.optDouble("product_star_rating", 0.0));
                product.setReviews(productJson.optInt("product_num_ratings", 0));
                products.add(product);
            }
            return products;

        } catch (IOException e) {
            System.err.println("Error getting product details: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public ProductDocument getProductDetails(String asin) {
        try {
            String url = "https://" + API_HOST + "/product-details?asin=" + asin + "&country=AE";
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
