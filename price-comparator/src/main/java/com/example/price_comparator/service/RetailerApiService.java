package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RetailerApiService {
    CompletableFuture<List<ProductDocument>> searchProducts(String query);
    CompletableFuture<ProductDocument> getProductDetails(String id);
}
