package com.example.price_comparator.service;

import com.example.price_comparator.dto.ShoppingProduct;
import com.example.price_comparator.model.ProductDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

@Service
public class ShoppingService {

    private static final Logger logger = LoggerFactory.getLogger(ShoppingService.class);
    private static final String GEO_LOCATION = "United Arab Emirates";

    @Autowired
    private OxylabsShoppingScraper oxylabsShoppingScraper;    public List<ShoppingProduct> findOffers(ProductDocument product, String username, String password, BiConsumer<Integer, String> progressCallback) {
        String searchQuery = buildSearchQuery(product);
        logger.info("Searching with targeted query: '{}'", searchQuery);
        return oxylabsShoppingScraper.scrapeShoppingResults(searchQuery, GEO_LOCATION, username, password, progressCallback);
    }

    /**
     * Find offers using enhanced scraping with detailed product specifications
     * This provides more accurate matching by fetching detailed specs for top products
     */
    public List<ShoppingProduct> findOffersEnhanced(ProductDocument product, String username, String password, BiConsumer<Integer, String> progressCallback) {
        String searchQuery = buildSearchQuery(product);
        logger.info("Searching with enhanced targeted query: '{}'", searchQuery);
        try {
            if (progressCallback != null) {
                progressCallback.accept(20, "Starting enhanced scraping with detailed specifications...");
            }
            return oxylabsShoppingScraper.scrapeShoppingResultsEnhanced(searchQuery, GEO_LOCATION, username, password);
        } catch (Exception e) {
            logger.error("Enhanced scraping failed, falling back to standard scraping: {}", e.getMessage());
            if (progressCallback != null) {
                progressCallback.accept(30, "Enhanced scraping failed, using standard method...");
            }
            // Fallback to standard scraping
            return oxylabsShoppingScraper.scrapeShoppingResults(searchQuery, GEO_LOCATION, username, password, progressCallback);
        }
    }

    private String buildSearchQuery(ProductDocument product) {
        StringJoiner queryBuilder = new StringJoiner(" ");

        // Start with the product name, cleaning it up a bit
        String name = product.getName();
        if (name.contains(":")) {
            name = name.substring(0, name.indexOf(":"));
        }
        queryBuilder.add(name);

        // Add other attributes if they exist
        if (product.getBrand() != null) {
            queryBuilder.add(product.getBrand());
        }
        if (product.getModel() != null) {
            queryBuilder.add(product.getModel());
        }
        if (product.getStorage() != null) {
            queryBuilder.add(product.getStorage());
        }
        if (product.getRam() != null) {
            queryBuilder.add(product.getRam());
        }
        if (product.getColor() != null) {
            queryBuilder.add(product.getColor());
        }
        
        String finalQuery = queryBuilder.toString();
        // Limit query length to avoid overly specific searches that might fail
        String[] words = finalQuery.trim().split("\\s+");
        if (words.length > 10) {
            finalQuery = String.join(" ", java.util.Arrays.copyOfRange(words, 0, 10));
        }
        return finalQuery;
    }
}