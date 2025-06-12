package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PriceApiServiceTest {

    @Autowired
    private PriceApiService priceApiService;

    @Test
    public void testFetchProductData() {
        // This is a live API test and requires a valid API key and network connection.
        // In a real-world scenario, you would mock the API response.
        String query = "B07ZPKBL9V";
        ProductDocument product = priceApiService.fetchProductData("amazon", query);

        // assertNotNull(product, "Product data should not be null");
        // assertNotNull(product.getName(), "Product name should not be null");
    }
}
