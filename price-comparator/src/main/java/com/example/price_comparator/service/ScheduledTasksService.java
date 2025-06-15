package com.example.price_comparator.service;

import com.example.price_comparator.model.PriceHistoryPoint;
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
                List<com.example.price_comparator.model.ProductDocument> products = amazonApiService.searchProducts(category);
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

    @Scheduled(cron = "0 0 * * * *") // Runs at the top of every hour
    public void updatePriceHistory() {
        logger.info("Starting scheduled price history update...");
        List<ProductDocument> allProducts = firebaseService.getAllProducts();

        for (ProductDocument product : allProducts) {
            try {
                logger.info("Checking price history for product: {}", product.getName());
                Optional<RetailerInfo> amazonRetailerOpt = product.getRetailers().stream()
                        .filter(r -> "amazon".equalsIgnoreCase(r.getRetailerId()))
                        .findFirst();

                if (amazonRetailerOpt.isPresent()) {
                    ProductDocument freshData = amazonApiService.getProductDetails(product.getId());
                    if (freshData != null && !freshData.getRetailers().isEmpty()) {
                        RetailerInfo freshAmazonRetailer = freshData.getRetailers().get(0);
                        RetailerInfo existingAmazonRetailer = amazonRetailerOpt.get();

                        if (freshAmazonRetailer.getCurrentPrice() != existingAmazonRetailer.getCurrentPrice()) {
                            logger.info("Price change detected for {}: old={}, new={}", product.getName(), existingAmazonRetailer.getCurrentPrice(), freshAmazonRetailer.getCurrentPrice());
                            
                            existingAmazonRetailer.setCurrentPrice(freshAmazonRetailer.getCurrentPrice());
                            
                            String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                            existingAmazonRetailer.getPriceHistory().add(new PriceHistoryPoint(currentDate, freshAmazonRetailer.getCurrentPrice()));
                            
                            productService.saveProduct(product);
                            logger.info("Updated price history for {}", product.getName());
                        }
                    }
                }
                // Wait before processing the next product to avoid rate limiting
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.error("Price history update was interrupted", e);
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                logger.error("Failed to update price history for product {}", product.getId(), e);
            }
        }
        logger.info("Finished scheduled price history update.");
    }
}
