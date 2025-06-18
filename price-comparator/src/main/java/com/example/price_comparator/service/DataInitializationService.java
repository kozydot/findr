package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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

    private final Executor delayer = CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        CompletableFuture.runAsync(() -> {
            logger.info("APPLICATION STARTUP - DATA INITIALIZATION CHECK");
            logger.info("========================================");

            firebaseService.getLastAmazonFetchTimestamp().thenCombine(firebaseService.productsExist(), (lastFetchTime, productsExist) -> {
                long currentTime = System.currentTimeMillis();
                long oneHourInMillis = TimeUnit.HOURS.toMillis(1);
                long timeSinceLastFetch = currentTime - lastFetchTime;
                
                logger.info("INITIALIZATION CRITERIA CHECK:");
                logger.info("  Products exist in database: {}", productsExist ? "YES" : "NO");
                
                if (lastFetchTime > 0) {
                    long hoursSinceLastFetch = timeSinceLastFetch / (1000 * 60 * 60);
                    logger.info("  Last fetch: {} hours ago", hoursSinceLastFetch);
                    logger.info("  Time threshold: 1 hour");
                } else {
                    logger.info("  Last fetch: Never");
                }

                boolean needsInit = !productsExist || (timeSinceLastFetch > oneHourInMillis);

                if (needsInit) {
                    logger.info("  DECISION: Initialization REQUIRED");
                    if (!productsExist) {
                        logger.info("  Reason: No products found in database");
                    } else {
                        logger.info("  Reason: Data is stale (older than 1 hour)");
                    }
                    logger.info("========================================");
                    performInitialization();
                } else {
                    logger.info("  DECISION: Initialization SKIPPED");
                    logger.info("  Reason: Data is fresh and exists");
                    logger.info("========================================");
                    logger.info("APPLICATION READY - Using existing data");
                }
                return null;
            }).exceptionally(ex -> {
                logger.error("Error during initialization check:", ex);
                logger.warn("Proceeding with initialization as fallback");
                performInitialization();
                return null;
            });
        });
    }

    private void performInitialization() {
        long startTime = System.currentTimeMillis();
        
        logger.info("STARTING DATA INITIALIZATION PROCESS");
        logger.info("====================================");
        
        List<String> categories = List.of("electronics", "computers", "smart home", "video games");
        logger.info("Categories to process: {}", categories);
        logger.info("Estimated time: ~{} minutes", categories.size() * 1.5);
        logger.info("Rate limit: 5 second delay between categories");
        
        CompletableFuture<Void> allCategoriesFuture = CompletableFuture.completedFuture(null);
        final int[] processedCategories = {0};
        final int[] totalProductsSaved = {0};

        for (String category : categories) {
            allCategoriesFuture = allCategoriesFuture.thenCompose(v -> {
                processedCategories[0]++;
                
                logger.info("PROCESSING CATEGORY {}/{}: {}",
                    processedCategories[0], categories.size(), category.toUpperCase());
                
                long categoryStartTime = System.currentTimeMillis();
                
                return amazonApiService.searchProductsByCategory(category)
                        .thenAccept(fetchedProducts -> {
                            long categoryEndTime = System.currentTimeMillis();
                            long categoryDuration = (categoryEndTime - categoryStartTime) / 1000;
                            
                            if (fetchedProducts != null && !fetchedProducts.isEmpty()) {
                                logger.info("Fetched {} products from Amazon in {}s",
                                    fetchedProducts.size(), categoryDuration);
                                logger.info("Saving products to database...");
                                
                                int savedCount = 0;
                                for (ProductDocument product : fetchedProducts) {
                                    try {
                                        productService.saveProduct(product);
                                        savedCount++;
                                    } catch (Exception e) {
                                        logger.warn("Failed to save product: {} - {}",
                                            product.getName(), e.getMessage());
                                    }
                                }
                                
                                totalProductsSaved[0] += savedCount;
                                logger.info("Successfully saved {}/{} products for category '{}'",
                                    savedCount, fetchedProducts.size(), category);
                                
                                if (savedCount < fetchedProducts.size()) {
                                    logger.warn("{} products failed to save",
                                        fetchedProducts.size() - savedCount);
                                }
                            } else {
                                logger.warn("No products fetched for category: {}", category);
                            }
                            
                            // Progress indicator
                            double progress = (double) processedCategories[0] / categories.size() * 100;
                            logger.info("Overall Progress: {:.1f}% ({}/{} categories)",
                                progress, processedCategories[0], categories.size());
                                
                            if (processedCategories[0] < categories.size()) {
                                logger.info("Waiting 5 seconds before next category...");
                            }
                        })
                        .thenCompose(v2 -> {
                            // Only delay if not the last category
                            if (processedCategories[0] < categories.size()) {
                                return CompletableFuture.runAsync(() -> {}, delayer);
                            } else {
                                return CompletableFuture.completedFuture(null);
                            }
                        });
            });
        }

        allCategoriesFuture.whenComplete((v, ex) -> {
            long endTime = System.currentTimeMillis();
            long totalDuration = (endTime - startTime) / 1000;
            
            logger.info("====================================");
            
            if (ex != null) {
                logger.error("DATA INITIALIZATION FAILED");
                logger.error("Duration: {}s", totalDuration);
                logger.error("Products saved before failure: {}", totalProductsSaved[0]);
                logger.error("Error details:", ex);
            } else {
                firebaseService.updateLastAmazonFetchTimestamp(startTime);
                
                logger.info("DATA INITIALIZATION COMPLETED SUCCESSFULLY!");
                logger.info("SUMMARY:");
                logger.info("  Total products saved: {}", totalProductsSaved[0]);
                logger.info("  Categories processed: {}/{}", processedCategories[0], categories.size());
                logger.info("  Total duration: {}m {}s", totalDuration / 60, totalDuration % 60);
                logger.info("  Average products per category: {}",
                    totalProductsSaved[0] > 0 ? totalProductsSaved[0] / categories.size() : 0);
                logger.info("  Next initialization: In 1 hour");
                logger.info("APPLICATION READY - Database populated with fresh data");
            }
            logger.info("====================================");
        });
    }
}