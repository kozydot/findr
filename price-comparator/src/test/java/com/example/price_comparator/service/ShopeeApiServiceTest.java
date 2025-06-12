package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ShopeeApiServiceTest {

    @Autowired
    private ShopeeApiService shopeeApiService;

    @Test
    public void testSearchProducts() {
        ProductDocument product = shopeeApiService.searchProducts("test");
        assertNull(product, "Product data should be null for placeholder");
    }
}
