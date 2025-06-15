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
            List<ProductDocument> products = amazonApiService.searchProducts(category);
            for (ProductDocument productSummary : products) {
                if (productSummary.getId() != null && !productSummary.getId().isEmpty()) {
                    try {
                        logger.info("Fetching full details for product: {}", productSummary.getId());
                        ProductDocument fullProductDetails = amazonApiService.getProductDetails(productSummary.getId());
                        if (fullProductDetails != null) {
                            productService.saveProduct(fullProductDetails);
                        }
                        // Wait for a short period to avoid rate limiting on the details endpoint
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        logger.error("Data initialization was interrupted during detail fetch wait", e);
                        Thread.currentThread().interrupt();
                        return; // Exit if interrupted
                    } catch (Exception e) {
                        logger.error("Failed to fetch or save details for product {}", productSummary.getId(), e);
                    }
                }
            }
        }

        firebaseService.updateLastAmazonFetchTimestamp(currentTime);
        logger.info("Data initialization complete.");
    }
}