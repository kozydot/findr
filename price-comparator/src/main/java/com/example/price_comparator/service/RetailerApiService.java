package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;

import java.util.List;

public interface RetailerApiService {
    List<ProductDocument> searchProducts(String query);
    ProductDocument getProductDetails(String id);
}
