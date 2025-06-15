package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.model.RetailerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class ScheduledTasksService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasksService.class);

    private final AmazonApiService amazonApiService;
    private final ProductService productService;
    private final FirebaseService firebaseService;

    @Autowired
    public ScheduledTasksService(AmazonApiService amazonApiService, ProductService productService, FirebaseService firebaseService) {
        this.amazonApiService = amazonApiService;
        this.productService = productService;
        this.firebaseService = firebaseService;
    }

    public void scheduleHourlyProductUpdate() {
        logger.info("Scheduled task: Starting hourly product update...");

        long lastFetchTime = firebaseService.getLastAmazonFetchTimestamp();
        long currentTime = System.currentTimeMillis();
        long oneHourInMillis = TimeUnit.HOURS.toMillis(1);

        if (currentTime - lastFetchTime < oneHourInMillis) {
            logger.info("Skipping Amazon API fetch as it was done within the last hour.");
            return;
        }

        List<String> categories = productService.getAmazonCategories();
        if (categories.isEmpty()) {
            logger.warn("No categories found to update.");
            return;
        }

        for (String category : categories) {
            try {
                logger.info("Attempting to update product data for category: {}", category);
                List<com.example.price_comparator.model.ProductDocument> products = amazonApiService.searchProductsByCategory(category).join();
                for (com.example.price_comparator.model.ProductDocument product : products) {
                    productService.saveProduct(product);
                }
                logger.info("Waiting for 10 seconds before next category update to avoid rate-limiting...");
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                logger.error("Scheduled task interrupted during wait", e);
                Thread.currentThread().interrupt();
            }
        }
        
        firebaseService.updateLastAmazonFetchTimestamp(currentTime);

        logger.info("Scheduled task: Hourly product update finished for all categories.");
    }

}
