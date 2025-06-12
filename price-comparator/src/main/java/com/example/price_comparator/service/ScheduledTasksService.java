package com.example.price_comparator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ScheduledTasksService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasksService.class);

    private final PriceApiService scrapingService;
    private final ProductService productService;
    private final FirebaseService firebaseService;

    @Autowired
    public ScheduledTasksService(PriceApiService scrapingService, ProductService productService, FirebaseService firebaseService) {
        this.scrapingService = scrapingService;
        this.productService = productService;
        this.firebaseService = firebaseService;
    }

    @Scheduled(fixedRate = 3600000, initialDelay = 5000) // Run every hour, with an initial 5-second delay
    public void scheduleHourlyProductUpdate() {
        logger.info("Scheduled task: Starting hourly product update...");

        long lastFetchTime = firebaseService.getLastAmazonFetchTimestamp();
        long currentTime = System.currentTimeMillis();
        long oneHourInMillis = TimeUnit.HOURS.toMillis(1);

        if (currentTime - lastFetchTime < oneHourInMillis) {
            logger.info("Skipping Amazon API fetch as it was done within the last hour.");
            return;
        }

        String category = "electronics";
        logger.info("Attempting to update product data for category: {}", category);
        productService.updateAllProducts(category);
        firebaseService.updateLastAmazonFetchTimestamp(currentTime);

        logger.info("Scheduled task: Hourly product update finished.");
    }
}
