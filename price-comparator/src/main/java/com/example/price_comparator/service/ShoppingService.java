package com.example.price_comparator.service;

import com.example.price_comparator.dto.ShoppingProduct;
import com.example.price_comparator.model.RetailerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShoppingService {

    private static final Logger logger = LoggerFactory.getLogger(ShoppingService.class);
    private static final String GEO_LOCATION = "United Arab Emirates";

    @Autowired
    private OxylabsShoppingScraper oxylabsShoppingScraper;

    public List<RetailerInfo> findOffers(String productName) {
        // Attempt 1: Search with the full product name
        List<ShoppingProduct> products = oxylabsShoppingScraper.scrapeShoppingResults(productName, GEO_LOCATION);

        // Attempt 2: If no offers found, try with a name truncated at the colon
        if (products.isEmpty()) {
            logger.info("No offers found for full name. Trying name truncated at colon.");
            String simplifiedName1 = productName;
            if (simplifiedName1.contains(":")) {
                simplifiedName1 = simplifiedName1.substring(0, simplifiedName1.indexOf(":"));
                products = oxylabsShoppingScraper.scrapeShoppingResults(simplifiedName1.trim(), GEO_LOCATION);
            }
        }

        // Attempt 3: If still no offers, try with the first 5 words of the truncated name
        if (products.isEmpty()) {
            logger.info("Still no offers. Trying first 5 words.");
            String simplifiedName2 = productName;
            if (simplifiedName2.contains(":")) {
                simplifiedName2 = simplifiedName2.substring(0, simplifiedName2.indexOf(":"));
            }
            
            String[] words = simplifiedName2.trim().split("\\s+");
            if (words.length > 5) {
                simplifiedName2 = String.join(" ", java.util.Arrays.copyOfRange(words, 0, 5));
                products = oxylabsShoppingScraper.scrapeShoppingResults(simplifiedName2, GEO_LOCATION);
            }
        }

        return products.stream().map(this::mapToRetailerInfo).collect(Collectors.toList());
    }

    private RetailerInfo mapToRetailerInfo(ShoppingProduct product) {
        RetailerInfo retailerInfo = new RetailerInfo();
        // Generate a simple unique ID
        retailerInfo.setRetailerId(product.getSeller() + "-" + product.getProductLink().hashCode());
        retailerInfo.setName(product.getSeller());
        retailerInfo.setCurrentPrice(product.getPrice());
        retailerInfo.setProductUrl(product.getProductLink());
        retailerInfo.setInStock(true); // Assuming in stock if it appears in search
        // Note: logo, priceHistory, freeShipping, shippingCost are not available from this scraper
        return retailerInfo;
    }
}