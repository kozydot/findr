package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.model.RetailerInfo;
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
    private final AmazonApiService amazonApiService;
    private final ShoppingService shoppingService;

    @Autowired
    public ProductService(FirebaseService firebaseService, AmazonApiService amazonApiService, ShoppingService shoppingService) {
        this.firebaseService = firebaseService;
        this.amazonApiService = amazonApiService;
        this.shoppingService = shoppingService;
    }

    public List<ProductDocument> getFeaturedProducts(int limit) {
        logger.info("Fetching {} featured products", limit);
        List<ProductDocument> allProducts = firebaseService.getAllProducts();
        return allProducts.subList(0, Math.min(limit, allProducts.size()));
    }

    public List<ProductDocument> getTrendingProducts() {
        logger.info("Fetching trending products");
        // This is a placeholder, you might want to implement a real trending logic
        return firebaseService.getAllProducts().subList(0, Math.min(10, firebaseService.getAllProducts().size()));
    }

    public Optional<ProductDocument> getProductById(String id) {
        logger.info("Fetching product by ID: {}", id);
        
        ProductDocument product = amazonApiService.getProductDetails(id);

        if (product == null) {
            logger.warn("Could not fetch product details from Amazon for ID: {}. Falling back to database.", id);
            try {
                product = firebaseService.getProduct(id).get();
                 if (product == null) {
                    logger.error("Product with ID {} not found in Firebase either.", id);
                    return Optional.empty();
                }
            } catch (Exception e) {
                logger.error("Error fetching product by ID from Firebase: {}", id, e);
                return Optional.empty();
            }
        }

        // Only perform comparison if there are no other retailers listed
        if (product.getRetailers().size() <= 1) {
            logger.info("Proceeding with Shopping comparison for: {}", product.getName());
            List<RetailerInfo> offers = shoppingService.findOffers(product.getName());
            if (!offers.isEmpty()) {
                logger.info("Found {} offers on Shopping for: {}", offers.size(), product.getName());
                
                // Get existing URLs to prevent duplicates
                // Combine existing and new offers
                List<RetailerInfo> allOffers = new java.util.ArrayList<>(product.getRetailers());
                allOffers.addAll(offers);

                // Deduplicate by retailer name, keeping the one with the lowest price
                java.util.Map<String, RetailerInfo> bestOffers = allOffers.stream()
                    .filter(offer -> offer.getName() != null && offer.getProductUrl() != null && !offer.getProductUrl().isEmpty())
                    .collect(java.util.stream.Collectors.toMap(
                        RetailerInfo::getName,
                        offer -> offer,
                        (offer1, offer2) -> offer1.getCurrentPrice() < offer2.getCurrentPrice() ? offer1 : offer2
                    ));
                
                // Sort the deduplicated offers by price and take the top 5
                List<RetailerInfo> finalOffers = bestOffers.values().stream()
                    .sorted(java.util.Comparator.comparingDouble(RetailerInfo::getCurrentPrice))
                    .limit(5)
                    .collect(Collectors.toList());

                product.setRetailers(finalOffers);
                logger.info("Saving {} unique offers.", finalOffers.size());
                saveProduct(product);
            } else {
                logger.warn("No Shopping offers found for: {}", product.getName());
            }
        } else {
            logger.info("Skipping Google Shopping search as comparison data already exists for: {}", product.getName());
        }

        return Optional.of(product);
    }

    public List<ProductDocument> getAllProducts() {
        logger.info("Fetching all products");
        List<ProductDocument> products = firebaseService.getAllProducts();
        products.forEach(this::retainOnlyAmazonRetailer);
        return products;
    }

    public List<ProductDocument> searchProducts(String query) {
        logger.info("Searching products with query: {}", query);
        List<ProductDocument> allProducts = firebaseService.getAllProducts();
        if (query == null || query.trim().isEmpty()) {
            allProducts.forEach(this::retainOnlyAmazonRetailer);
            return allProducts;
        }
        String lowerCaseQuery = query.toLowerCase();
        List<ProductDocument> filteredProducts = allProducts.stream()
                .filter(p -> p.getName().toLowerCase().contains(lowerCaseQuery))
                .collect(java.util.stream.Collectors.toList());
        filteredProducts.forEach(this::retainOnlyAmazonRetailer);
        return filteredProducts;
    }

    private void retainOnlyAmazonRetailer(ProductDocument product) {
        if (product.getRetailers() != null) {
            List<RetailerInfo> amazonRetailers = product.getRetailers().stream()
                .filter(r -> "amazon".equals(r.getRetailerId()))
                .collect(Collectors.toList());
            product.setRetailers(amazonRetailers);
        }
    }

    public List<String> getAmazonCategories() {
        logger.info("Fetching categories from Amazon");
        return List.of("Electronics", "Computers", "Smart Home", "Video Games");
    }

    public ProductDocument saveProduct(ProductDocument product) {
        logger.info("Saving product to Firebase: {}", product.getName());
        firebaseService.saveProduct(product);
        return product;
    }
}
