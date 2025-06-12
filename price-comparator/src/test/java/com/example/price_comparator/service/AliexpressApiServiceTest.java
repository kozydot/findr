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
        java.util.List<ProductDocument> products = aliexpressApiService.searchProducts("test");
        assertNotNull(products, "Product data should not be null");
        assertFalse(products.isEmpty(), "Product list should not be empty");
        assertNotNull(products.get(0).getName(), "Product name should not be null");
    }
}
