package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import org.springframework.stereotype.Service;

@Service
public class PriceApiService {

    private final AmazonApiService amazonApiService;
    private final AliexpressApiService aliexpressApiService;

    public PriceApiService(AmazonApiService amazonApiService, AliexpressApiService aliexpressApiService) {
        this.amazonApiService = amazonApiService;
        this.aliexpressApiService = aliexpressApiService;
    }

    public ProductDocument fetchProductData(String retailer, String query) {
        switch (retailer.toLowerCase()) {
            case "amazon":
                return amazonApiService.searchProducts(query);
            case "aliexpress":
                return aliexpressApiService.searchProducts(query);
            default:
                return null;
        }
    }
}
