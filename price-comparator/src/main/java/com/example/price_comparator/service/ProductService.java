package com.example.price_comparator.service;

import com.example.price_comparator.dto.ShoppingProduct;
import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.model.RetailerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final FirebaseService firebaseService;
    private final AmazonApiService amazonApiService;
    private final ShoppingService shoppingService;

    @Value("${oxylabs.username}")
    private String username;

    @Value("${oxylabs.password}")
    private String password;

    @Autowired
    public ProductService(FirebaseService firebaseService, AmazonApiService amazonApiService, ShoppingService shoppingService) {
        this.firebaseService = firebaseService;
        this.amazonApiService = amazonApiService;
        this.shoppingService = shoppingService;
    }

    public List<ProductDocument> getFeaturedProducts(int limit) {
        logger.info("Fetching {} featured products", limit);
        List<ProductDocument> allProducts = firebaseService.getAllProducts();
        allProducts.forEach(this::retainOnlyAmazonRetailer);
        return allProducts.subList(0, Math.min(limit, allProducts.size()));
    }

    public List<ProductDocument> getTrendingProducts() {
        logger.info("Fetching trending products");
        List<ProductDocument> allProducts = firebaseService.getAllProducts();
        allProducts.forEach(this::retainOnlyAmazonRetailer);
        return allProducts.subList(0, Math.min(10, allProducts.size()));
    }

    public Optional<ProductDocument> getProductById(String id) {
        logger.info("Fetching product by ID: {}", id);
        try {
            ProductDocument product;
            try {
                product = firebaseService.getProduct(id).get();
            } catch (Exception e) {
                logger.warn("Product {} not found in Firebase or error fetching: {}", id, e.getMessage());
                product = null;
            }

            if (product == null) {
                logger.info("Product not found in Firebase. Fetching from Amazon API.");
                product = amazonApiService.getProductDetails(id);
                if (product == null) {
                    logger.error("Product with ID {} not found in Amazon either.", id);
                    return Optional.empty();
                }
            } else {
                logger.info("Product found in Firebase. Checking if details are complete.");
                if (product.getDescription() == null || product.getDescription().isEmpty() || product.getSpecifications() == null || product.getSpecifications().isEmpty()) {
                    logger.info("Enriching existing product with full details from Amazon.");
                    ProductDocument fullDetails = amazonApiService.getProductDetails(product.getId());
                    if (fullDetails != null) {
                        if (product.getDescription() == null || product.getDescription().isEmpty()) {
                            product.setDescription(fullDetails.getDescription());
                        }
                        if (product.getSpecifications() == null || product.getSpecifications().isEmpty()) {
                            product.setSpecifications(fullDetails.getSpecifications());
                        }
                        if (product.getAbout() == null || product.getAbout().isEmpty()) {
                            product.setAbout(fullDetails.getAbout());
                        }
                    }
                }
            }

            final ProductDocument finalProduct = product;

            if (finalProduct.getRetailers() == null || finalProduct.getRetailers().size() <= 1) {
                logger.info("Proceeding with Shopping comparison for: {}", finalProduct.getName());
                List<ShoppingProduct> shoppingProducts = shoppingService.findOffers(finalProduct.getName(), username, password);
                if (!shoppingProducts.isEmpty()) {
                    logger.info("Found {} offers on Shopping for: {}", shoppingProducts.size(), finalProduct.getName());

                    // This logic is now redundant as we fetch from Amazon directly.
                    // However, we can keep it as a fallback for non-Amazon products if needed in the future.
                    if (finalProduct.getDescription() == null || finalProduct.getDescription().isEmpty() || finalProduct.getSpecifications() == null || finalProduct.getSpecifications().isEmpty()) {
                        shoppingProducts.stream()
                            .filter(p -> (p.getDescription() != null && !p.getDescription().isEmpty()) || (p.getSpecifications() != null && !p.getSpecifications().isEmpty()))
                            .findFirst()
                            .ifPresent(p -> {
                                if (finalProduct.getDescription() == null || finalProduct.getDescription().isEmpty()) {
                                    finalProduct.setDescription(p.getDescription());
                                    logger.info("Updated product description from shopping results as a fallback.");
                                }
                                if (finalProduct.getSpecifications() == null || finalProduct.getSpecifications().isEmpty()) {
                                    finalProduct.setSpecifications(p.getSpecifications());
                                    logger.info("Updated product specifications from shopping results as a fallback.");
                                }
                            });
                    }

                    List<RetailerInfo> offers = shoppingProducts.stream()
                        .map(this::mapToRetailerInfo)
                        .collect(Collectors.toList());

                    List<RetailerInfo> allOffers = new ArrayList<>();
                    if (finalProduct.getRetailers() != null) {
                        allOffers.addAll(finalProduct.getRetailers());
                    }
                    allOffers.addAll(offers);

                    java.util.Map<String, RetailerInfo> bestOffers = allOffers.stream()
                        .filter(offer -> offer.getName() != null && offer.getProductUrl() != null && !offer.getProductUrl().isEmpty())
                        .collect(Collectors.toMap(
                            RetailerInfo::getName,
                            offer -> offer,
                            (offer1, offer2) -> offer1.getCurrentPrice() < offer2.getCurrentPrice() ? offer1 : offer2
                        ));
                    
                    List<RetailerInfo> finalOffers = bestOffers.values().stream()
                        .sorted(java.util.Comparator.comparingDouble(RetailerInfo::getCurrentPrice))
                        .limit(5)
                        .collect(Collectors.toList());

                    finalProduct.setRetailers(finalOffers);
                    logger.info("Saving {} unique offers.", finalOffers.size());
                    saveProduct(finalProduct);
                } else {
                    logger.warn("No Shopping offers found for: {}", finalProduct.getName());
                }
            } else {
                logger.info("Skipping Shopping search as comparison data already exists for: {}", finalProduct.getName());
            }
            
            return Optional.of(finalProduct);

        } catch (Exception e) {
            logger.error("Error fetching product by ID: {}", id, e);
            return Optional.empty();
        }
    }

    public List<ProductDocument> getAllProducts() {
        logger.info("Fetching all products");
        List<ProductDocument> products = firebaseService.getAllProducts();
        products.forEach(this::retainOnlyAmazonRetailer);
        return products;
    }

    public List<ProductDocument> searchProducts(String query) {
        logger.info("Searching products with query: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return getAllProducts();
        }
        
        List<ProductDocument> searchResults = amazonApiService.searchProducts(query);
        
        return searchResults.parallelStream()
            .map(productSummary -> {
                try {
                    // First, check if a fully detailed product exists in Firebase
                    Optional<ProductDocument> existingProductOpt = firebaseService.getProduct(productSummary.getId())
                        .thenApply(Optional::ofNullable)
                        .exceptionally(ex -> {
                            logger.warn("Failed to fetch product {} from Firebase, will fetch from Amazon.", productSummary.getId());
                            return Optional.empty();
                        }).join();

                    if (existingProductOpt.isPresent() && existingProductOpt.get().getDescription() != null) {
                        logger.info("Found complete product {} in cache.", productSummary.getId());
                        return existingProductOpt.get();
                    }

                    // If not, fetch full details from Amazon
                    logger.info("Fetching full details for product {}.", productSummary.getId());
                    ProductDocument detailedProduct = amazonApiService.getProductDetails(productSummary.getId());
                    if (detailedProduct != null) {
                        saveProduct(detailedProduct);
                        return detailedProduct;
                    }
                    return null;
                } catch (Exception e) {
                    logger.error("Failed to process product {}", productSummary.getId(), e);
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
    }

    private void retainOnlyAmazonRetailer(ProductDocument product) {
        if (product.getRetailers() != null) {
            List<RetailerInfo> amazonRetailers = product.getRetailers().stream()
                .filter(r -> "amazon".equalsIgnoreCase(r.getRetailerId()) || "Amazon.ae".equalsIgnoreCase(r.getName()))
                .collect(Collectors.toList());
            product.setRetailers(amazonRetailers);
        }
    }

    private RetailerInfo mapToRetailerInfo(ShoppingProduct product) {
        RetailerInfo retailerInfo = new RetailerInfo();
        retailerInfo.setRetailerId(product.getSeller() + "-" + product.getProductLink().hashCode());
        retailerInfo.setName(product.getSeller());
        retailerInfo.setCurrentPrice(product.getPrice());
        retailerInfo.setProductUrl(product.getProductLink());
        retailerInfo.setInStock(true); // Assuming in stock if it appears in search
        return retailerInfo;
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
