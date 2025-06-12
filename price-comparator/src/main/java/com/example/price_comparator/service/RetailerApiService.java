package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;

public interface RetailerApiService {
    ProductDocument searchProducts(String query);
    ProductDocument getProductDetails(String id);
}
