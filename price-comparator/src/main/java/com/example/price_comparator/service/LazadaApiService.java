package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import org.springframework.stereotype.Service;

@Service
public class LazadaApiService implements RetailerApiService {

    @Override
    public ProductDocument searchProducts(String query) {
        // Placeholder for Lazada API implementation
        return null;
    }

    @Override
    public ProductDocument getProductDetails(String id) {
        // Placeholder for Lazada API implementation
        return null;
    }
}
