package com.example.price_comparator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTasksService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasksService.class);

    private final PriceApiService scrapingService;
    private final ProductService productService;

    @Autowired
    public ScheduledTasksService(PriceApiService scrapingService, ProductService productService) {
        this.scrapingService = scrapingService;
        this.productService = productService;
    }

    @Scheduled(fixedRate = 3600000, initialDelay = 5000) // Run every hour, with an initial 5-second delay
    public void scheduleHourlyProductUpdate() {
        logger.info("Scheduled task: Starting hourly product update...");

        // In a real application, you would get a list of products to update
        // from the database. For this example, we'll use a hardcoded query.
        String exampleQuery = "iphone"; 

        logger.info("Attempting to update product data for query: {}", exampleQuery);
        productService.updateAllProducts(exampleQuery);

        logger.info("Scheduled task: Hourly product update finished.");
    }
}
