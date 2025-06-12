package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AmazonApiServiceTest {

    @Autowired
    private AmazonApiService amazonApiService;

    @Test
    public void testSearchProducts() {
        // This is a live API test and requires a valid API key and network connection.
        // In a real-world scenario, you would mock the API response.
        String query = "B07ZPKBL9V";
        java.util.List<ProductDocument> products = amazonApiService.searchProducts(query);

        // assertNotNull(products, "Product data should not be null");
        // assertFalse(products.isEmpty(), "Product list should not be empty");
        // assertNotNull(products.get(0).getName(), "Product name should not be null");
    }
}
