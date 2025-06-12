package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import org.springframework.stereotype.Service;

@Service
public class PriceApiService {

    private final AmazonApiService amazonApiService;
    private final ShopeeApiService shopeeApiService;
    private final LazadaApiService lazadaApiService;
    private final AliexpressApiService aliexpressApiService;

    public PriceApiService(AmazonApiService amazonApiService, ShopeeApiService shopeeApiService, LazadaApiService lazadaApiService, AliexpressApiService aliexpressApiService) {
        this.amazonApiService = amazonApiService;
        this.shopeeApiService = shopeeApiService;
        this.lazadaApiService = lazadaApiService;
        this.aliexpressApiService = aliexpressApiService;
    }

    public ProductDocument fetchProductData(String retailer, String query) {
        switch (retailer.toLowerCase()) {
            case "amazon":
                return amazonApiService.searchProducts(query);
            case "shopee":
                return shopeeApiService.searchProducts(query);
            case "lazada":
                return lazadaApiService.searchProducts(query);
            case "aliexpress":
                return aliexpressApiService.searchProducts(query);
            default:
                return null;
        }
    }
}
