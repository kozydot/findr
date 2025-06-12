package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ShopeeApiService implements RetailerApiService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private static final String API_KEY = "789a3d99e9mshf91eeac2b820feap133038jsnf9a117afcd70";
    private static final String API_HOST = "shopee14.p.rapidapi.com";

    @Override
    public ProductDocument searchProducts(String query) {
        // Placeholder for Shopee API implementation
        return null;
    }

    @Override
    public ProductDocument getProductDetails(String id) {
        // Placeholder for Shopee API implementation
        return null;
    }
}
