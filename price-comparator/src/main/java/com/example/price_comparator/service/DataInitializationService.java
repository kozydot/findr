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
            logger.info("Checking if data initialization is needed...");

            firebaseService.getLastAmazonFetchTimestamp().thenCombine(firebaseService.productsExist(), (lastFetchTime, productsExist) -> {
                long currentTime = System.currentTimeMillis();
                long oneHourInMillis = TimeUnit.HOURS.toMillis(1);

                boolean needsInit = !productsExist || (currentTime - lastFetchTime > oneHourInMillis);

                if (needsInit) {
                    performInitialization();
                } else {
                    logger.info("Skipping data initialization as it was performed recently and data exists.");
                }
                return null;
            }).exceptionally(ex -> {
                logger.error("An error occurred during data initialization check. Assuming initialization is needed.", ex);
                performInitialization();
                return null;
            });
        });
    }

    private void performInitialization() {
        long currentTime = System.currentTimeMillis();
        logger.info("Starting data initialization...");
        List<String> categories = List.of("electronics", "computers", "smart home", "video games");

        CompletableFuture<Void> allCategoriesFuture = CompletableFuture.completedFuture(null);

        for (String category : categories) {
            allCategoriesFuture = allCategoriesFuture.thenCompose(v -> {
                logger.info("Fetching products for category: {}", category);
                return amazonApiService.searchProductsByCategory(category)
                        .thenAccept(fetchedProducts -> {
                            for (ProductDocument product : fetchedProducts) {
                                productService.saveProduct(product);
                            }
                        })
                        .thenCompose(v2 -> CompletableFuture.runAsync(() -> {}, delayer));
            });
        }

        allCategoriesFuture.whenComplete((v, ex) -> {
            if (ex != null) {
                logger.error("Data initialization failed", ex);
            } else {
                firebaseService.updateLastAmazonFetchTimestamp(currentTime);
                logger.info("Data initialization complete.");
            }
        });
    }
}