package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PriceApiService {

    private final AmazonApiService amazonApiService;
    private final AliexpressApiService aliexpressApiService;

    public PriceApiService(AmazonApiService amazonApiService, AliexpressApiService aliexpressApiService) {
        this.amazonApiService = amazonApiService;
        this.aliexpressApiService = aliexpressApiService;
    }

    public List<ProductDocument> fetchProductData(String retailer, String query) {
        switch (retailer.toLowerCase()) {
            case "amazon":
                return amazonApiService.searchProducts(query);
            case "aliexpress":
                return aliexpressApiService.searchProducts(query);
            default:
                return null;
        }
    }

    public ProductDocument getProductDetails(String retailer, String id) {
        switch (retailer.toLowerCase()) {
            case "amazon":
                return amazonApiService.getProductDetails(id);
            case "aliexpress":
                return aliexpressApiService.getProductDetails(id);
            default:
                return null;
        }
    }
}
