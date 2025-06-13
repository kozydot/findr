package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final FirebaseService firebaseService;
    private final PriceApiService priceApiService;
    private final AmazonApiService amazonApiService;

    @Autowired
    public ProductService(FirebaseService firebaseService, PriceApiService priceApiService, AmazonApiService amazonApiService) {
        this.firebaseService = firebaseService;
        this.priceApiService = priceApiService;
        this.amazonApiService = amazonApiService;
    }

    public List<ProductDocument> getFeaturedProducts(int limit) {
        logger.info("Fetching {} featured products", limit);
        List<ProductDocument> allProducts = firebaseService.getAllProducts();
        return allProducts.subList(0, Math.min(limit, allProducts.size()));
    }

    public List<ProductDocument> getTrendingProducts() {
        logger.info("Fetching trending products");
        return priceApiService.fetchProductData("amazon", "electronics");
    }

    public Optional<ProductDocument> getProductById(String id) {
        logger.info("Fetching product by ID: {}", id);
        try {
            return Optional.ofNullable(firebaseService.getProduct(id).get());
        } catch (Exception e) {
            logger.error("Error fetching product by ID: {}", id, e);
            return Optional.empty();
        }
    }

    public List<ProductDocument> getAllProducts() {
        logger.info("Fetching all products");
        return firebaseService.getAllProducts();
    }

    public List<ProductDocument> searchProducts(String query) {
        logger.info("Searching products with query: {}", query);
        List<ProductDocument> allProducts = firebaseService.getAllProducts();
        if (query == null || query.trim().isEmpty()) {
            return allProducts;
        }
        String lowerCaseQuery = query.toLowerCase();
        return allProducts.stream()
                .filter(product -> product.getName().toLowerCase().contains(lowerCaseQuery))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<String> getAmazonCategories() {
        logger.info("Fetching categories from Amazon");
        // This is a placeholder. In a real application, you would have a mechanism
        // to fetch and store categories from Amazon.
        return List.of("Electronics", "Computers", "Smart Home", "Video Games");
    }

    public ProductDocument saveProduct(ProductDocument product) {
        logger.info("Saving product to Firebase: {}", product.getName());
        firebaseService.saveProduct(product);
        return product;
    }

    public ProductDocument compareAndSaveProduct(String productId) {
        Optional<ProductDocument> productOptional = getProductById(productId);
        if (productOptional.isEmpty()) {
            return null;
        }

        ProductDocument product = productOptional.get();
        
        ProductDocument amazonProduct = priceApiService.getProductDetails("amazon", productId);
        if (amazonProduct != null) {
            product.getRetailers().addAll(amazonProduct.getRetailers());
        }

        ProductDocument aliexpressProduct = priceApiService.getProductDetails("aliexpress", productId);
        if (aliexpressProduct != null) {
            product.getRetailers().addAll(aliexpressProduct.getRetailers());
        }

        return saveProduct(product);
    }

    public void updateAllProducts(String query) {
        logger.info("Updating all products for query: {}", query);
        List<ProductDocument> productSummaries = priceApiService.fetchProductData("amazon", query);

        if (productSummaries == null || productSummaries.isEmpty()) {
            logger.warn("No product summaries found for query: {}", query);
            return;
        }

        List<CompletableFuture<Void>> futures = productSummaries.stream()
                .filter(summary -> summary != null && summary.getId() != null)
                .map(summary -> CompletableFuture.runAsync(() -> {
                    try {
                        Optional<ProductDocument> existingProductOpt = getProductById(summary.getId());
                        long twentyFourHoursInMillis = 24 * 60 * 60 * 1000;

                        if (existingProductOpt.isPresent()) {
                            ProductDocument existingProduct = existingProductOpt.get();
                            if (existingProduct.getLastChecked() != null &&
                                (new Date().getTime() - existingProduct.getLastChecked().getTime()) < twentyFourHoursInMillis) {
                                logger.info("Skipping recently checked product: {}", existingProduct.getName());
                                return;
                            }
                            // Product exists, merge data
                            existingProduct.setPrice(summary.getPrice());
                            existingProduct.setRating(summary.getRating());
                            existingProduct.setReviews(summary.getReviews());
                            existingProduct.setLastChecked(new Date());
                            saveProduct(existingProduct);
                            logger.info("Updated product: {}", existingProduct.getName());
                        } else {
                            // Product does not exist, fetch full details and save
                            ProductDocument fullProductDetails = amazonApiService.getProductDetails(summary.getId());
                            if (fullProductDetails != null) {
                                fullProductDetails.setLastChecked(new Date());
                                saveProduct(fullProductDetails);
                                logger.info("Saved new product: {}", fullProductDetails.getName());
                            } else {
                                logger.warn("Could not fetch full details for new product with ID: {}", summary.getId());
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error processing product with ID: {}", summary.getId(), e);
                    }
                }))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
