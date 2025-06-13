package com.example.price_comparator.service;

import com.example.price_comparator.dto.ComparisonResult;
import com.example.price_comparator.model.AliexpressProduct;
import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.model.SpecificationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
public class ProductMatchingServiceTest {

    @Autowired
    private ProductMatchingService productMatchingService;

    @MockBean
    private AliexpressApiService aliexpressApiService;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @MockBean
    private ImageHashingService imageHashingService;

    @Test
    public void testFindAndCompare_SuccessfulMatch() {
        // Arrange
        ProductDocument amazonProduct = new ProductDocument();
        amazonProduct.setName("Redragon GS560 Adjudicator RGB Desktop Gaming Speakers");
        amazonProduct.setPrice("150.00");
        amazonProduct.setImageUrl("amazon_image_url");
        amazonProduct.setSpecifications(List.of(new SpecificationInfo("Brand", "Redragon"), new SpecificationInfo("Model Number", "GS560")));

        AliexpressProduct aliexpressProduct = new AliexpressProduct();
        aliexpressProduct.setProductTitle("Redragon GS560 Speakers");
        aliexpressProduct.setSalePrice(35.0);
        aliexpressProduct.setMainImageUrl("aliexpress_image_url");
        aliexpressProduct.setLatestSaleVolume(100);
        aliexpressProduct.setShopName("Redragon Official Store");
        aliexpressProduct.setProductDetailUrl("aliexpress_url");

        when(aliexpressApiService.searchProducts(anyString(), anyString(), anyString())).thenReturn(Collections.singletonList(aliexpressProduct));
        when(exchangeRateService.getUsdToAedRate()).thenReturn(3.67);
        when(imageHashingService.getPHash("amazon_image_url")).thenReturn("hash1");
        when(imageHashingService.getPHash("aliexpress_image_url")).thenReturn("hash1"); // Same hash for perfect match
        when(imageHashingService.calculateHammingDistance("hash1", "hash1")).thenReturn(0);

        // Act
        ComparisonResult result = productMatchingService.findAndCompare(amazonProduct);

        // Assert
        assertNotNull(result);
        assertTrue(result.isMatchFound());
        assertNotNull(result.getAliexpressMatch());
        assertEquals("Redragon GS560 Speakers", result.getAliexpressMatch().getProductTitle());
    }
}