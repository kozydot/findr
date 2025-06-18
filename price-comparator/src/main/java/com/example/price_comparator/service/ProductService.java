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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
        List<ProductDocument> allProducts = firebaseService.getAllProducts().join();
        allProducts.forEach(this::retainOnlyAmazonRetailer);
        return allProducts.subList(0, Math.min(limit, allProducts.size()));
    }

    public List<ProductDocument> getTrendingProducts() {
        logger.info("Fetching trending products");
        List<ProductDocument> allProducts = firebaseService.getAllProducts().join();
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
                    logger.info("SHOPPING RESULTS PROCESSING - Product: {}",
                        product.getName().length() > 60 ? product.getName().substring(0, 57) + "..." : product.getName());
                    logger.info("Found {} potential offers from shopping search", shoppingProducts.size());
                    
                    // Product matching phase
                    logger.info("PRODUCT MATCHING PHASE - Processing {} offers", shoppingProducts.size());
                    
                    List<RetailerInfo> offers = shoppingProducts.parallelStream()
                        .filter(scrapedProduct -> isMatch(product, scrapedProduct))
                        .map(this::mapToRetailerInfo)
                        .collect(Collectors.toList());
                    
                    logger.info("MATCHING COMPLETE - Accepted {} offers after filtering", offers.size());
                    
                    List<RetailerInfo> allOffers = new ArrayList<>();
                    if (product.getRetailers() != null) {
                        allOffers.addAll(product.getRetailers());
                        logger.info("Added {} existing offers from database", product.getRetailers().size());
                    }
                    allOffers.addAll(offers);
                    
                    // Deduplication phase
                    logger.info("DEDUPLICATION PHASE - Processing {} total offers", allOffers.size());
                    
                    // Enhanced deduplication logging
                    java.util.Map<String, java.util.List<RetailerInfo>> groupedOffers = new java.util.HashMap<>();
                    allOffers.forEach(offer -> {
                        if (offer.getName() != null && offer.getProductUrl() != null) {
                            String normalizedUrl = normalizeUrl(offer.getProductUrl());
                            String normalizedRetailer = normalizeRetailerName(offer.getName());
                            String key = normalizedRetailer + "|" + normalizedUrl;
                            
                            groupedOffers.computeIfAbsent(key, k -> new ArrayList<>()).add(offer);
                        }
                    });
                    
                    logger.info("Generated {} unique keys for deduplication", groupedOffers.size());
                    
                    // Log duplicates found
                    int duplicateCount = 0;
                    for (java.util.Map.Entry<String, java.util.List<RetailerInfo>> entry : groupedOffers.entrySet()) {
                        if (entry.getValue().size() > 1) {
                            duplicateCount++;
                            double bestPrice = entry.getValue().stream().mapToDouble(RetailerInfo::getCurrentPrice).min().orElse(0.0);
                            logger.info("DUPLICATE FOUND - Key: {} | Best Price: {}",
                                entry.getKey().length() > 50 ? entry.getKey().substring(0, 47) + "..." : entry.getKey(),
                                String.format("%.2f", bestPrice));
                        }
                    }
                    
                    if (duplicateCount == 0) {
                        logger.info("No duplicates detected - all offers are unique");
                    }
                    
                    java.util.Map<String, RetailerInfo> bestOffers = allOffers.stream()
                        .filter(offer -> offer.getName() != null && offer.getProductUrl() != null && !offer.getProductUrl().isEmpty())
                        .collect(Collectors.toMap(
                            offer -> normalizeRetailerName(offer.getName()) + "|" + normalizeUrl(offer.getProductUrl()),
                            offer -> offer,
                            (offer1, offer2) -> offer1.getCurrentPrice() < offer2.getCurrentPrice() ? offer1 : offer2
                        ));
                        
                    logger.info("DEDUPLICATION COMPLETE - {} unique offers remain", bestOffers.size());
                    
                    List<RetailerInfo> finalOffers = bestOffers.values().stream()
                        .sorted(java.util.Comparator.comparingDouble(RetailerInfo::getCurrentPrice))
                        .limit(5)
                        .collect(Collectors.toList());
                    
                    // Final results
                    logger.info("FINAL RESULTS - Saving {} offers:", finalOffers.size());
                    finalOffers.forEach((offer) -> {
                        String shortUrl = offer.getProductUrl().length() > 40 ?
                                offer.getProductUrl().substring(0, 37) + "..." : offer.getProductUrl();
                        logger.info("  {} | Price: {} | URL: {}",
                            offer.getName(), String.format("%.2f", offer.getCurrentPrice()), shortUrl);
                    });
                    
                    product.setRetailers(finalOffers);
                    logger.info("Successfully saved {} offers to database", finalOffers.size());
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
        List<ProductDocument> products = firebaseService.getAllProducts().join();
        products.forEach(this::retainOnlyAmazonRetailer);
        return products;
    }

    public List<ProductDocument> searchProducts(String query) {
        logger.info("Searching products with query: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return getAllProducts();
        }

        // First, search in Firebase
        List<ProductDocument> localResults = firebaseService.searchProductsByName(query).join();
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

    private boolean isMatch(ProductDocument originalProduct, ShoppingProduct scrapedProduct) {
        if (originalProduct == null || scrapedProduct == null || scrapedProduct.getTitle() == null) {
            return false;
        }

        // Create a detailed string for the original product
        StringBuilder originalDetails = new StringBuilder();
        originalDetails.append(originalProduct.getName());
        if (originalProduct.getBrand() != null) {
            originalDetails.append(" ").append(originalProduct.getBrand());
        }
        if (originalProduct.getModel() != null) {
            originalDetails.append(" ").append(originalProduct.getModel());
        }

        // Create a detailed string for the scraped product
        StringBuilder scrapedDetailsBuilder = new StringBuilder();
        scrapedDetailsBuilder.append(scrapedProduct.getTitle());
        if (scrapedProduct.getDescription() != null) {
            scrapedDetailsBuilder.append(" ").append(scrapedProduct.getDescription());
        }
        if (scrapedProduct.getSpecifications() != null) {
            for (com.example.price_comparator.model.SpecificationInfo spec : scrapedProduct.getSpecifications()) {
                scrapedDetailsBuilder.append(" ").append(spec.getName()).append(" ").append(spec.getValue());
            }
        }
        String scrapedDetails = scrapedDetailsBuilder.toString();

        String originalText = originalDetails.toString().toLowerCase();
        String scrapedText = scrapedDetails.toLowerCase();
        
        // Intelligent matching approach using specification comparison
        double totalScore = 0.0;
        double maxScore = 0.0;
        
        // 1. Core text similarity (40% weight)
        org.apache.commons.text.similarity.JaroWinklerSimilarity jaroWinkler = new org.apache.commons.text.similarity.JaroWinklerSimilarity();
        double jaroWinklerScore = jaroWinkler.apply(originalText, scrapedText);
        totalScore += jaroWinklerScore * 0.4;
        maxScore += 0.4;
        
        // 2. Brand matching (20% weight)
        double brandScore = calculateBrandScore(originalProduct, scrapedText);
        totalScore += brandScore * 0.2;
        maxScore += 0.2;
        
        // 3. Specification-based matching (30% weight) - Universal approach
        double specScore = calculateSpecificationScore(originalProduct, scrapedProduct);
        totalScore += specScore * 0.3;
        maxScore += 0.3;
        
        // 4. Key terms overlap (10% weight)
        double keyTermScore = calculateKeyTermScore(originalText, scrapedText);
        totalScore += keyTermScore * 0.1;
        maxScore += 0.1;
        
        double finalScore = maxScore > 0 ? totalScore / maxScore : 0.0;
        boolean matches = finalScore > 0.65;
        
        // Clean structured logging for product matching
        String result = matches ? "ACCEPTED" : "REJECTED";
        
        logger.debug("PRODUCT MATCH: {} | Score: {} | Original: {} | Scraped: {}",
            result,
            String.format("%.3f", finalScore),
            originalDetails.toString().length() > 40 ?
                originalDetails.toString().substring(0, 37) + "..." : originalDetails.toString(),
            scrapedProduct.getTitle().length() > 40 ?
                scrapedProduct.getTitle().substring(0, 37) + "..." : scrapedProduct.getTitle());
                
        // Only log detailed breakdown for matches or close misses
        if (matches || finalScore > 0.5) {
            logger.info("MATCH ANALYSIS - {} (Score: {})", result, String.format("%.3f", finalScore));
            logger.info("  Text: {} | Brand: {} | Specs: {} | Terms: {}",
                String.format("%.3f", jaroWinklerScore),
                String.format("%.3f", brandScore),
                String.format("%.3f", specScore),
                String.format("%.3f", keyTermScore));
        }
        
        return matches;
    }
    
    /**
     * Universal brand scoring that works across all product categories
     */
    private double calculateBrandScore(ProductDocument originalProduct, String scrapedText) {
        String brand = null;
        if (originalProduct.getBrand() != null && !originalProduct.getBrand().isEmpty()) {
            brand = originalProduct.getBrand().toLowerCase();
        } else {
            // Extract brand from product name
            brand = extractUniversalBrand(originalProduct.getName());
        }
        
        if (brand != null && scrapedText.contains(brand)) {
            return 1.0;
        }
        return 0.0;
    }
    
    /**
     * Universal specification-based matching using available spec data
     */
    private double calculateSpecificationScore(ProductDocument originalProduct, ShoppingProduct scrapedProduct) {
        if (originalProduct.getSpecifications() == null || originalProduct.getSpecifications().isEmpty()) {
            // If no specs available for original, use high-level attribute matching
            return calculateAttributeScore(originalProduct, scrapedProduct);
        }
        
        int matchingSpecs = 0;
        int totalSpecs = 0;
        
        // Compare specifications when available
        for (com.example.price_comparator.model.SpecificationInfo originalSpec : originalProduct.getSpecifications()) {
            totalSpecs++;
            String specName = originalSpec.getName().toLowerCase();
            String specValue = originalSpec.getValue().toLowerCase();
            
            // Check if scraped product has matching specification
            if (scrapedProduct.getSpecifications() != null) {
                for (com.example.price_comparator.model.SpecificationInfo scrapedSpec : scrapedProduct.getSpecifications()) {
                    if (scrapedSpec.getName().toLowerCase().contains(specName) ||
                        specName.contains(scrapedSpec.getName().toLowerCase())) {
                        
                        if (areSpecValuesCompatible(specValue, scrapedSpec.getValue().toLowerCase())) {
                            matchingSpecs++;
                            break;
                        }
                    }
                }
            }
        }
        
        if (totalSpecs == 0) {
            return calculateAttributeScore(originalProduct, scrapedProduct);
        }
        
        return (double) matchingSpecs / totalSpecs;
    }
    
    /**
     * Fallback attribute matching for products without detailed specifications
     */
    private double calculateAttributeScore(ProductDocument originalProduct, ShoppingProduct scrapedProduct) {
        double score = 0.0;
        int attributes = 0;
        
        String scrapedText = (scrapedProduct.getTitle() + " " +
                             (scrapedProduct.getDescription() != null ? scrapedProduct.getDescription() : "")).toLowerCase();
        
        // Check key attributes that are commonly available
        if (originalProduct.getColor() != null) {
            attributes++;
            if (scrapedText.contains(originalProduct.getColor().toLowerCase())) {
                score += 1.0;
            }
        }
        
        if (originalProduct.getStorage() != null) {
            attributes++;
            if (scrapedText.contains(originalProduct.getStorage().toLowerCase())) {
                score += 1.0;
            }
        }
        
        if (originalProduct.getRam() != null) {
            attributes++;
            if (scrapedText.contains(originalProduct.getRam().toLowerCase())) {
                score += 1.0;
            }
        }
        
        if (originalProduct.getModel() != null) {
            attributes++;
            if (scrapedText.contains(originalProduct.getModel().toLowerCase())) {
                score += 1.0;
            }
        }
        
        return attributes > 0 ? score / attributes : 0.7; // Default moderate score if no attributes
    }
    
    /**
     * Check if specification values are compatible (handles variants)
     */
    private boolean areSpecValuesCompatible(String original, String scraped) {
        // Exact match
        if (original.equals(scraped)) {
            return true;
        }
        
        // Numeric tolerance for measurements (10% tolerance)
        try {
            double origVal = extractNumericValue(original);
            double scrapedVal = extractNumericValue(scraped);
            if (origVal > 0 && scrapedVal > 0) {
                double tolerance = Math.max(origVal, scrapedVal) * 0.1;
                return Math.abs(origVal - scrapedVal) <= tolerance;
            }
        } catch (Exception e) {
            // Continue with text comparison
        }
        
        // Partial match for complex values
        return original.contains(scraped) || scraped.contains(original);
    }
    
    /**
     * Extract numeric value from specification string
     */
    private double extractNumericValue(String value) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([0-9]+\\.?[0-9]*)");
        java.util.regex.Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }
    
    /**
     * Calculate score based on key term overlap (important words)
     */
    private double calculateKeyTermScore(String originalText, String scrapedText) {
        // Extract important terms (nouns, adjectives, numbers)
        Set<String> originalKeyTerms = extractKeyTerms(originalText);
        Set<String> scrapedKeyTerms = extractKeyTerms(scrapedText);
        
        if (originalKeyTerms.isEmpty()) {
            return 0.5; // Default score
        }
        
        Set<String> intersection = new HashSet<>(originalKeyTerms);
        intersection.retainAll(scrapedKeyTerms);
        
        return (double) intersection.size() / originalKeyTerms.size();
    }
    
    /**
     * Extract key terms from product text
     */
    private Set<String> extractKeyTerms(String text) {
        Set<String> keyTerms = new HashSet<>();
        String[] words = text.toLowerCase().split("\\W+");
        
        // Common stop words to exclude
        Set<String> stopWords = Set.of("the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were", "been", "be", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should", "may", "might", "can", "shall");
        
        for (String word : words) {
            if (word.length() > 2 && !stopWords.contains(word)) {
                // Include numbers, brand-like words, and significant terms
                if (word.matches(".*\\d.*") || word.length() > 4 || isLikelyBrandOrModel(word)) {
                    keyTerms.add(word);
                }
            }
        }
        
        return keyTerms;
    }
    
    /**
     * Check if a word is likely a brand or model identifier
     */
    private boolean isLikelyBrandOrModel(String word) {
        // Contains mix of letters and numbers, or is capitalized in original context
        return word.matches(".*[0-9].*[a-z].*") || word.matches(".*[a-z].*[0-9].*") ||
               word.matches("^[A-Z][a-z]+$");
    }
    
    /**
     * Universal brand extraction that works for any product category
     */
    private String extractUniversalBrand(String productName) {
        if (productName == null || productName.isEmpty()) {
            return null;
        }
        
        // Extract first significant word as potential brand
        String[] words = productName.split("\\s+");
        if (words.length > 0) {
            String firstWord = words[0].replaceAll("[^a-zA-Z]", "").toLowerCase();
            if (firstWord.length() > 2) {
                return firstWord;
            }
        }
        
        return null;
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

    /**
     * Normalizes retailer names to handle variations
     */
    private String normalizeRetailerName(String retailerName) {
        if (retailerName == null) {
            return "";
        }
        
        String normalized = retailerName.toLowerCase().trim();
        
        // Handle Amazon variations
        if (normalized.contains("amazon")) {
            return "amazon";
        }
        
        return normalized;
    }

    /**
     * Normalizes URLs to handle duplicate detection by removing tracking parameters
     * and standardizing format
     */
    private String normalizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        
        try {
            URI uri = new URI(url);
            
            // Build normalized URL with scheme, host, and path
            StringBuilder normalized = new StringBuilder();
            if (uri.getScheme() != null) {
                normalized.append(uri.getScheme().toLowerCase()).append("://");
            }
            if (uri.getHost() != null) {
                String host = uri.getHost().toLowerCase();
                // Normalize host variations (remove www, handle subdomains)
                if (host.startsWith("www.")) {
                    host = host.substring(4);
                }
                normalized.append(host);
            }
            if (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443) {
                normalized.append(":").append(uri.getPort());
            }
            if (uri.getPath() != null) {
                String path = uri.getPath();
                
                // Special handling for different URL patterns from same retailer
                path = normalizeRetailerSpecificPaths(path, uri.getHost());
                
                // Remove trailing slash unless it's the root path
                if (path.length() > 1 && path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                normalized.append(path);
            }
            
            // Process query parameters, excluding common tracking parameters
            if (uri.getQuery() != null) {
                String[] params = uri.getQuery().split("&");
                List<String> filteredParams = Arrays.stream(params)
                    .filter(param -> !isTrackingParameter(param))
                    .sorted() // Sort parameters for consistency
                    .collect(Collectors.toList());
                
                if (!filteredParams.isEmpty()) {
                    normalized.append("?");
                    normalized.append(String.join("&", filteredParams));
                }
            }
            
            String result = normalized.toString();
            logger.debug("🔗 URL NORMALIZED: {} -> {}",
                url.length() > 50 ? url.substring(0, 47) + "..." : url,
                result.length() > 50 ? result.substring(0, 47) + "..." : result);
            return result;
            
        } catch (URISyntaxException e) {
            logger.warn("Failed to normalize URL: {}, using fallback normalization", url);
            // Fallback normalization
            String fallback = url.toLowerCase().trim();
            
            // Remove www prefix
            fallback = fallback.replaceFirst("://www\\.", "://");
            
            // Remove trailing slash
            if (fallback.endsWith("/") && fallback.length() > 1) {
                fallback = fallback.substring(0, fallback.length() - 1);
            }
            // Remove common query parameters manually as fallback
            if (fallback.contains("?")) {
                String[] parts = fallback.split("\\?", 2);
                String basePart = parts[0];
                String queryPart = parts[1];
                
                String[] params = queryPart.split("&");
                List<String> filteredParams = Arrays.stream(params)
                    .filter(param -> !isTrackingParameter(param))
                    .sorted()
                    .collect(Collectors.toList());
                
                if (!filteredParams.isEmpty()) {
                    fallback = basePart + "?" + String.join("&", filteredParams);
                } else {
                    fallback = basePart;
                }
            }
            logger.debug("🔄 URL Fallback Normalization: {} -> {}",
                url.length() > 50 ? url.substring(0, 47) + "..." : url,
                fallback.length() > 50 ? fallback.substring(0, 47) + "..." : fallback);
            return fallback;
        }
    }
    
    /**
     * Normalizes retailer-specific path patterns to handle different URL structures
     * for the same products
     */
    private String normalizeRetailerSpecificPaths(String path, String host) {
        if (path == null || host == null) {
            return path;
        }
        
        String lowerHost = host.toLowerCase();
        String lowerPath = path.toLowerCase();
        
        // Handle Cartlow different URL patterns
        if (lowerHost.contains("cartlow")) {
            // Extract product identifier from both URL formats
            // Format 1: /listing/uae/en/dmFyMTAwNDUxODAyMjhfQ0FSVExPVw==
            // Format 2: /uae/en/pdp/id10536648/apple-iphone-13-512gb-blue.html
            
            if (lowerPath.contains("/listing/")) {
                // Extract the base64 encoded ID from listing URLs
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/listing/[^/]+/[^/]+/([^/]+)");
                java.util.regex.Matcher matcher = pattern.matcher(lowerPath);
                if (matcher.find()) {
                    return "/cartlow-product/" + matcher.group(1);
                }
            } else if (lowerPath.contains("/pdp/")) {
                // Extract product ID from PDP URLs
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/pdp/([^/]+)/");
                java.util.regex.Matcher matcher = pattern.matcher(lowerPath);
                if (matcher.find()) {
                    return "/cartlow-product/" + matcher.group(1);
                }
            }
        }
        
        // Handle Amazon different URL patterns
        if (lowerHost.contains("amazon")) {
            // Extract ASIN from Amazon URLs - handles multiple patterns including product names in path
            // ASIN can be uppercase or lowercase, so use case-insensitive pattern
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*?/dp/([A-Za-z0-9]{10})", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(lowerPath);
            if (matcher.find()) {
                String asin = matcher.group(1).toUpperCase(); // Normalize to uppercase
                logger.debug("🏷️  Amazon ASIN extracted: {} from /dp/ pattern", asin);
                return "/dp/" + asin;
            }
            
            // Alternative pattern for /gp/product/ URLs
            java.util.regex.Pattern altPattern = java.util.regex.Pattern.compile("/gp/product/([A-Za-z0-9]{10})", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher altMatcher = altPattern.matcher(lowerPath);
            if (altMatcher.find()) {
                String asin = altMatcher.group(1).toUpperCase(); // Normalize to uppercase
                logger.debug("🏷️  Amazon ASIN extracted: {} from /gp/product/ pattern", asin);
                return "/dp/" + asin;
            }
            
            logger.warn("⚠️  Could not extract ASIN from Amazon path: {}",
                lowerPath.length() > 50 ? lowerPath.substring(0, 47) + "..." : lowerPath);
        }
        
        return path;
    }
    
    /**
     * Checks if a query parameter is a tracking parameter that should be removed
     */
    private boolean isTrackingParameter(String param) {
        if (param == null || param.isEmpty()) {
            return true;
        }
        
        String paramName = param.split("=")[0].toLowerCase();
        
        // Common tracking parameters to remove
        return paramName.startsWith("utm_") ||
               paramName.startsWith("ref") ||
               paramName.equals("tag") ||
               paramName.equals("linkcode") ||
               paramName.equals("camp") ||
               paramName.equals("creative") ||
               paramName.equals("creativeASIN") ||
               paramName.equals("ascsubtag") ||
               paramName.equals("source") ||
               paramName.equals("medium") ||
               paramName.equals("campaign") ||
               paramName.equals("fbclid") ||
               paramName.equals("gclid") ||
               paramName.equals("msclkid") ||
               paramName.equals("_encoding") ||
               paramName.equals("psc") ||               // Amazon price/shipping calculation
               paramName.equals("smid") ||              // Amazon seller/merchant ID - causes duplicates
               paramName.equals("pd_rd_i") ||           // Amazon recommendation tracking
               paramName.equals("pd_rd_r") ||           // Amazon recommendation tracking
               paramName.equals("pd_rd_w") ||           // Amazon recommendation tracking
               paramName.equals("pd_rd_wg") ||          // Amazon recommendation tracking
               paramName.equals("pf_rd_i") ||           // Amazon product finder tracking
               paramName.equals("pf_rd_m") ||           // Amazon product finder tracking
               paramName.equals("pf_rd_p") ||           // Amazon product finder tracking
               paramName.equals("pf_rd_r") ||           // Amazon product finder tracking
               paramName.equals("pf_rd_s") ||           // Amazon product finder tracking
               paramName.equals("pf_rd_t");             // Amazon product finder tracking
    }
    
    /**
     * Extracts brand name from product title when brand field is not available
     */
    private String extractBrandFromName(String productName) {
        if (productName == null || productName.isEmpty()) {
            return null;
        }
        
        String lowerName = productName.toLowerCase();
        
        // Common electronics brands
        String[] knownBrands = {"apple", "samsung", "razer", "logitech", "hp", "dell", "asus", "acer", "lenovo",
                               "sony", "lg", "microsoft", "google", "amazon", "intel", "amd", "nvidia", "corsair",
                               "steelseries", "hyperx", "bose", "beats", "jbl", "xiaomi", "huawei", "oneplus"};
        
        for (String brand : knownBrands) {
            if (lowerName.contains(brand)) {
                return brand;
            }
        }
        
        // If no known brand found, try to extract first word as potential brand
        String[] words = productName.split("\\s+");
        if (words.length > 0) {
            String firstWord = words[0].toLowerCase().replaceAll("[^a-zA-Z]", "");
            if (firstWord.length() > 2) {
                return firstWord;
            }
        }
        
        return null;
    }
    
    /**
     * Extracts model from product title when model field is not available
     */
    private String extractModelFromName(String productName) {
        if (productName == null || productName.isEmpty()) {
            return null;
        }
        
        String lowerName = productName.toLowerCase();
        
        // Look for common model patterns
        // For DeathAdder, extract "deathadder essential"
        if (lowerName.contains("deathadder")) {
            if (lowerName.contains("essential")) {
                return "deathadder essential";
            } else if (lowerName.contains("v2")) {
                return "deathadder v2";
            } else if (lowerName.contains("v3")) {
                return "deathadder v3";
            }
            return "deathadder";
        }
        
        // For iPhones, extract iPhone model
        if (lowerName.contains("iphone")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("iphone\\s*(\\d+[\\w\\s]*(?:pro|max|mini|plus)?)", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(lowerName);
            if (matcher.find()) {
                return "iphone " + matcher.group(1).trim();
            }
            return "iphone";
        }
        
        // For Galaxy phones
        if (lowerName.contains("galaxy")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("galaxy\\s*([\\w\\d\\s]*)", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(lowerName);
            if (matcher.find()) {
                return "galaxy " + matcher.group(1).split("\\s")[0];
            }
            return "galaxy";
        }
        
        // Look for alphanumeric model patterns (e.g., "G502", "MX518")
        java.util.regex.Pattern alphaNumPattern = java.util.regex.Pattern.compile("\\b[A-Z]+\\d+[A-Z]*\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher alphaNumMatcher = alphaNumPattern.matcher(productName);
        if (alphaNumMatcher.find()) {
            return alphaNumMatcher.group().toLowerCase();
        }
        
        return null;
    }
    
    /**
     * Calculates penalty for product variants (different colors, storage, etc.)
     * Returns 0.0 for same product, higher values for different variants
     */
    private double calculateVariantPenalty(String originalText, String scrapedText) {
        // Enhanced color variations with more comprehensive detection
        String[] colors = {"black", "white", "blue", "red", "green", "yellow", "pink", "purple", "gray", "grey",
                          "gold", "silver", "rose", "graphite", "midnight", "starlight", "space", "coral", "mint",
                          "sierra", "alpine", "deep", "dark", "light", "bright", "natural"};
        String originalColor = null;
        String scrapedColor = null;
        
        // More aggressive color detection - match whole words to avoid false positives
        for (String color : colors) {
            if (originalText.matches(".*\\b" + color + "\\b.*")) {
                originalColor = color;
                break; // Take first match to avoid conflicts
            }
        }
        for (String color : colors) {
            if (scrapedText.matches(".*\\b" + color + "\\b.*")) {
                scrapedColor = color;
                break; // Take first match to avoid conflicts
            }
        }
        
        // Enhanced storage variations
        String[] storages = {"16gb", "32gb", "64gb", "128gb", "256gb", "512gb", "1tb", "2tb", "4tb"};
        String originalStorage = null;
        String scrapedStorage = null;
        
        for (String storage : storages) {
            if (originalText.contains(storage)) {
                originalStorage = storage;
                break; // Take first match
            }
        }
        for (String storage : storages) {
            if (scrapedText.contains(storage)) {
                scrapedStorage = storage;
                break; // Take first match
            }
        }
        
        // Model variations (Pro vs regular, Mini, Plus, Max)
        boolean originalIsPro = originalText.contains("pro");
        boolean scrapedIsPro = scrapedText.contains("pro");
        boolean originalIsMini = originalText.contains("mini");
        boolean scrapedIsMini = scrapedText.contains("mini");
        boolean originalIsPlus = originalText.contains("plus");
        boolean scrapedIsPlus = scrapedText.contains("plus");
        boolean originalIsMax = originalText.contains("max");
        boolean scrapedIsMax = scrapedText.contains("max");
        
        double penalty = 0.0;
        
        // Heavy penalty for different model variants
        if (originalIsPro != scrapedIsPro) {
            penalty += 0.8; // 80% penalty - Pro vs regular is a major difference
            logger.debug("Model variant penalty: Pro vs regular mismatch");
        }
        if (originalIsMini != scrapedIsMini) {
            penalty += 0.8; // 80% penalty - Mini vs regular is a major difference
            logger.debug("Model variant penalty: Mini vs regular mismatch");
        }
        if (originalIsPlus != scrapedIsPlus) {
            penalty += 0.8; // 80% penalty - Plus vs regular is a major difference
            logger.debug("Model variant penalty: Plus vs regular mismatch");
        }
        if (originalIsMax != scrapedIsMax) {
            penalty += 0.8; // 80% penalty - Max vs regular is a major difference
            logger.debug("Model variant penalty: Max vs regular mismatch");
        }
        
        // High penalty for different colors (customers care about color)
        if (originalColor != null && scrapedColor != null && !originalColor.equals(scrapedColor)) {
            penalty += 0.7; // 70% penalty - increased significantly
            logger.debug("Color variant penalty: {} vs {}", originalColor, scrapedColor);
        }
        
        // High penalty for different storage (major spec difference)
        if (originalStorage != null && scrapedStorage != null && !originalStorage.equals(scrapedStorage)) {
            penalty += 0.8; // 80% penalty - increased significantly
            logger.debug("Storage variant penalty: {} vs {}", originalStorage, scrapedStorage);
        }
        
        // Additional check for condition variations (New vs Renewed/Refurbished)
        boolean originalIsRenewed = originalText.contains("renewed") || originalText.contains("refurbished") ||
                                   originalText.contains("used") || originalText.contains("pre-owned");
        boolean scrapedIsRenewed = scrapedText.contains("renewed") || scrapedText.contains("refurbished") ||
                                  scrapedText.contains("used") || scrapedText.contains("pre-owned");
        
        if (originalIsRenewed != scrapedIsRenewed) {
            penalty += 0.5; // 50% penalty for condition mismatch
            logger.debug("Condition variant penalty: Renewed vs New mismatch");
        }
        
        // Cap penalty at 0.95 (95%) to allow for some flexibility but be very strict
        return Math.min(penalty, 0.95);
    }
}
