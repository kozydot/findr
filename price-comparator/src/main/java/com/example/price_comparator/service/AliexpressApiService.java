package com.example.price_comparator.service;

import com.example.price_comparator.model.AliexpressProduct;
import com.example.price_comparator.model.ProductDocument;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AliexpressApiService implements RetailerApiService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private static final String API_KEY = "b1aec85babmsh6428821d9bebf86p1df2e5jsnb037ea881482";
    private static final String API_HOST = "aliexpress-product-data-api.p.rapidapi.com";

    public List<AliexpressProduct> searchProducts(String query, String shipToCountry, String currency) {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host(API_HOST)
                .addPathSegment("search")
                .addQueryParameter("searchTerm", query)
                .addQueryParameter("shipToCountry", shipToCountry)
                .addQueryParameter("currency", currency)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-key", API_KEY)
                .addHeader("x-rapidapi-host", API_HOST)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("API request failed with code: " + response.code());
                return new ArrayList<>();
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray productsJson = jsonResponse.getJSONArray("products");

            List<AliexpressProduct> products = new ArrayList<>();
            for (int i = 0; i < productsJson.length(); i++) {
                JSONObject productJson = productsJson.getJSONObject(i);
                AliexpressProduct product = new AliexpressProduct();
                product.setProductTitle(productJson.getString("productTitle"));
                product.setProductDetailUrl(productJson.getString("productDetailUrl"));
                product.setMainImageUrl(productJson.getString("mainImageUrl"));
                product.setSalePrice(productJson.getDouble("salePrice"));
                product.setShopName(productJson.getString("shopName"));
                product.setLatestSaleVolume(productJson.getInt("latestSaleVolume"));
                products.add(product);
            }
            return products;

        } catch (IOException | org.json.JSONException e) {
            System.err.println("Error searching Aliexpress products: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<ProductDocument> searchProducts(String query) {
        // This method is part of the RetailerApiService interface, but our new logic
        // requires more parameters. We'll leave it unimplemented or adapt the interface.
        // For now, it returns an empty list to satisfy the contract.
        return new ArrayList<>();
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
