package com.example.price_comparator.service;

import com.example.price_comparator.dto.ShoppingProduct;
import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.model.RetailerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final Set<String> comparisonInProgress = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, CompletableFuture<ProductDocument>> comparisonResults = new ConcurrentHashMap<>();
    private final FirebaseService firebaseService;
    private final AmazonApiService amazonApiService;
    private final ShoppingService shoppingService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${oxylabs.username}")
    private String username;

    @Value("${oxylabs.password}")
    private String password;

    @Autowired
    public ProductService(FirebaseService firebaseService, AmazonApiService amazonApiService, ShoppingService shoppingService, SimpMessagingTemplate messagingTemplate) {
        this.firebaseService = firebaseService;
        this.amazonApiService = amazonApiService;
        this.shoppingService = shoppingService;
        this.messagingTemplate = messagingTemplate;
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

    public CompletableFuture<ProductDocument> getProductById(String id) {
        return firebaseService.getProduct(id);
    }

    public CompletableFuture<ProductDocument> fetchAndSaveProduct(String id) {
        return amazonApiService.getProductDetails(id)
            .thenApply(product -> {
                if (product != null) {
                    saveProduct(product);
                }
                return product;
            });
    }

    public String startShoppingComparison(ProductDocument product) {
        String taskId = UUID.randomUUID().toString();
        CompletableFuture<ProductDocument> future = triggerShoppingComparison(product, taskId);
        comparisonResults.put(taskId, future);
        return taskId;
    }

    @Async("taskExecutor")
    public CompletableFuture<ProductDocument> enrichProduct(ProductDocument product) {
        if (needsEnrichment(product)) {
            logger.info("Enriching existing product with full details from Amazon.");
            return amazonApiService.getProductDetails(product.getId())
                .thenApply(fullDetails -> {
                    if (fullDetails != null) {
                        updateProductDetails(product, fullDetails);
                        saveProduct(product);
                        messagingTemplate.convertAndSend("/topic/products/" + product.getId(), product);
                    }
                    return product;
                });
        }
        return CompletableFuture.completedFuture(product);
    }

    private boolean needsEnrichment(ProductDocument product) {
        return product.getDescription() == null || product.getDescription().isEmpty() ||
               product.getModel() == null || product.getStorage() == null || product.getRam() == null;
    }

    private void updateProductDetails(ProductDocument existingProduct, ProductDocument newDetails) {
        if (newDetails.getDescription() != null && !newDetails.getDescription().isEmpty()) {
            existingProduct.setDescription(newDetails.getDescription());
        }
        if (newDetails.getSpecifications() != null && !newDetails.getSpecifications().isEmpty()) {
            existingProduct.setSpecifications(newDetails.getSpecifications());
        }
        if (newDetails.getAbout() != null && !newDetails.getAbout().isEmpty()) {
            existingProduct.setAbout(newDetails.getAbout());
        }
        if (newDetails.getModel() != null) {
            existingProduct.setModel(newDetails.getModel());
        }
        if (newDetails.getStorage() != null) {
            existingProduct.setStorage(newDetails.getStorage());
        }
        if (newDetails.getRam() != null) {
            existingProduct.setRam(newDetails.getRam());
        }
        if (newDetails.getColor() != null) {
            existingProduct.setColor(newDetails.getColor());
        }
    }

    public Optional<ProductDocument> getComparisonResult(String taskId) {
        CompletableFuture<ProductDocument> future = comparisonResults.get(taskId);
        if (future != null && future.isDone()) {
            comparisonResults.remove(taskId);
            return Optional.ofNullable(future.join());
        }
        return Optional.empty();
    }

    @Async("taskExecutor")
    public CompletableFuture<ProductDocument> triggerShoppingComparison(ProductDocument product, String taskId) {
        return CompletableFuture.supplyAsync(() -> {
            String progressTopic = "/topic/products/" + product.getId() + "/progress";

            if (product.getRetailers() == null || product.getRetailers().size() <= 1) {
                logger.info("Proceeding with Shopping comparison for: {}", product.getName());
                
                java.util.function.BiConsumer<Integer, String> progressCallback = (progress, message) -> {
                    logger.info("Progress for {}: {}% - {}", product.getId(), progress, message);
                    messagingTemplate.convertAndSend(progressTopic, progress);
                };

                List<ShoppingProduct> shoppingProducts = shoppingService.findOffers(product, username, password, progressCallback);

                if (!shoppingProducts.isEmpty()) {
                    logger.info("Found {} offers on Shopping for: {}", shoppingProducts.size(), product.getName());
                    List<RetailerInfo> offers = shoppingProducts.parallelStream()
                        .filter(scrapedProduct -> isMatch(product.getName(), scrapedProduct.getTitle()))
                        .map(this::mapToRetailerInfo)
                        .collect(Collectors.toList());
                    List<RetailerInfo> allOffers = new ArrayList<>();
                    if (product.getRetailers() != null) {
                        allOffers.addAll(product.getRetailers());
                    }
                    allOffers.addAll(offers);
                    java.util.Map<String, RetailerInfo> bestOffers = allOffers.stream()
                        .filter(offer -> offer.getName() != null && offer.getProductUrl() != null && !offer.getProductUrl().isEmpty())
                        .collect(Collectors.toMap(
                            offer -> offer.getName().toLowerCase(),
                            offer -> offer,
                            (offer1, offer2) -> offer1.getCurrentPrice() < offer2.getCurrentPrice() ? offer1 : offer2
                        ));
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
                logger.info("Skipping Shopping search as comparison data already exists for: {}", product.getName());
                messagingTemplate.convertAndSend(progressTopic, 100);
            }

            messagingTemplate.convertAndSend("/topic/products/" + product.getId(), product);
            return product;
        });
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

        // First, search in Firebase
        List<ProductDocument> localResults = firebaseService.searchProductsByName(query);
        if (!localResults.isEmpty()) {
            logger.info("Found {} products in Firebase for query: {}", localResults.size(), query);
            localResults.forEach(this::retainOnlyAmazonRetailer);
            return localResults;
        }

        // If not found in Firebase, then search Amazon
        logger.info("No products found in Firebase for query: {}. Searching Amazon...", query);
        List<ProductDocument> searchResults = amazonApiService.searchProducts(query).join();
        searchResults.forEach(this::saveProduct);
        return searchResults;
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

    private boolean isMatch(String originalTitle, String scrapedTitle) {
        if (originalTitle == null || scrapedTitle == null) {
            return false;
        }
        org.apache.commons.text.similarity.JaroWinklerSimilarity jaroWinkler = new org.apache.commons.text.similarity.JaroWinklerSimilarity();
        double similarity = jaroWinkler.apply(originalTitle.toLowerCase(), scrapedTitle.toLowerCase());
        return similarity > 0.85; // Threshold can be adjusted
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

    public void addBookmark(String userId, String productId) {
        firebaseService.addBookmark(userId, productId);
    }

    public void removeBookmark(String userId, String productId) {
        firebaseService.removeBookmark(userId, productId);
    }

    public CompletableFuture<List<ProductDocument>> getBookmarks(String userId) {
        return firebaseService.getBookmarks(userId).thenCompose(productIds -> {
            List<CompletableFuture<ProductDocument>> futures = productIds.stream()
                    .map(this::getProductById)
                    .collect(Collectors.toList());
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .filter(java.util.Objects::nonNull)
                            .collect(Collectors.toList()));
        });
    }
}
