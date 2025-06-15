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

    public List<ShoppingProduct> findOffers(String productName, String username, String password) {
        String simplifiedName = productName;
        if (simplifiedName.contains(":")) {
            simplifiedName = simplifiedName.substring(0, simplifiedName.indexOf(":"));
        }
        
        String[] words = simplifiedName.trim().split("\\s+");
        if (words.length > 5) {
            simplifiedName = String.join(" ", java.util.Arrays.copyOfRange(words, 0, 5));
        }

        logger.info("Searching with simplified product name: '{}'", simplifiedName);
        return oxylabsShoppingScraper.scrapeShoppingResults(simplifiedName, GEO_LOCATION, username, password);
    }
}