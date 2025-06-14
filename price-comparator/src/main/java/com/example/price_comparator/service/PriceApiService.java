package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PriceApiService {

    private final AmazonApiService amazonApiService;

    public PriceApiService(AmazonApiService amazonApiService) {
        this.amazonApiService = amazonApiService;
    }

    public List<ProductDocument> fetchProductData(String retailer, String query) {
        if ("amazon".equalsIgnoreCase(retailer)) {
            return amazonApiService.searchProducts(query);
        }
        return null;
    }

    public ProductDocument getProductDetails(String retailer, String id) {
        if ("amazon".equalsIgnoreCase(retailer)) {
            return amazonApiService.getProductDetails(id);
        }
        return null;
    }
}