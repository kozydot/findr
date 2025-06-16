package com.example.price_comparator.service;

import com.example.price_comparator.dto.ShoppingProduct;
import com.example.price_comparator.model.ProductDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

@Service
public class ShoppingService {

    private static final Logger logger = LoggerFactory.getLogger(ShoppingService.class);
    private static final String GEO_LOCATION = "United Arab Emirates";

    @Autowired
    private OxylabsShoppingScraper oxylabsShoppingScraper;

    public List<ShoppingProduct> findOffers(ProductDocument product, String username, String password) {
        String searchQuery = buildSearchQuery(product);
        logger.info("Searching with targeted query: '{}'", searchQuery);
        return oxylabsShoppingScraper.scrapeShoppingResults(searchQuery, GEO_LOCATION, username, password);
    }

    private String buildSearchQuery(ProductDocument product) {
        StringJoiner queryBuilder = new StringJoiner(" ");
        if (product.getBrand() != null && product.getModel() != null) {
            queryBuilder.add(product.getBrand());
            queryBuilder.add(product.getModel());
            if (product.getStorage() != null) {
                queryBuilder.add(product.getStorage());
            }
            if (product.getRam() != null) {
                queryBuilder.add(product.getRam());
            }
            if (product.getColor() != null) {
                queryBuilder.add(product.getColor());
            }
            return queryBuilder.toString();
        }

        String name = product.getName();
        if (name.contains(":")) {
            name = name.substring(0, name.indexOf(":"));
        }
        String[] words = name.trim().split("\\s+");
        if (words.length > 7) {
            name = String.join(" ", java.util.Arrays.copyOfRange(words, 0, 7));
        }
        return name;
    }
}