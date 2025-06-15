package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

import java.util.List;

@Service
public class DataInitializationService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializationService.class);

    @Autowired
    private AmazonApiService amazonApiService;

    @Autowired
    private ProductService productService;

    @Autowired
    private FirebaseService firebaseService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Checking if data initialization is needed...");

        long lastFetchTime = firebaseService.getLastAmazonFetchTimestamp();
        long currentTime = System.currentTimeMillis();
        long oneHourInMillis = TimeUnit.HOURS.toMillis(1);

        if (currentTime - lastFetchTime < oneHourInMillis) {
            logger.info("Skipping data initialization as it was performed within the last hour.");
            return;
        }

        logger.info("Starting data initialization...");
        List<String> categories = List.of("electronics", "computers", "smart home", "video games");

        for (String category : categories) {
            logger.info("Fetching products for category: {}", category);
            List<ProductDocument> products = amazonApiService.searchProductsByCategory(category);
            for (ProductDocument product : products) {
                productService.saveProduct(product);
            }
            try {
                // Wait for 5 seconds before the next category to avoid rate limiting
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.error("Data initialization was interrupted during wait", e);
                Thread.currentThread().interrupt();
            }
        }

        firebaseService.updateLastAmazonFetchTimestamp(currentTime);
        logger.info("Data initialization complete.");
    }
}