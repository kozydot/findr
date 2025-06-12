package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AliexpressApiServiceTest {

    @Autowired
    private AliexpressApiService aliexpressApiService;

    @Test
    public void testSearchProducts() {
        ProductDocument product = aliexpressApiService.searchProducts("test");
        assertNotNull(product, "Product data should not be null");
        assertEquals("Aliexpress API Test Product", product.getName());
    }
}
