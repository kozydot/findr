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
    private final ConcurrentHashMap<String, CompletableFuture<ProductDocument>> comparisonResults = new ConcurrentHashMap<>();    private final FirebaseService firebaseService;
    private final AmazonApiService amazonApiService;
    private final ShoppingService shoppingService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${oxylabs.username}")
    private String username;

    @Value("${oxylabs.password}")
    private String password;    @Autowired
    public ProductService(FirebaseService firebaseService, AmazonApiService amazonApiService, 
                         ShoppingService shoppingService, SimpMessagingTemplate messagingTemplate) {
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

                // Use enhanced scraping for better specification matching
                List<ShoppingProduct> shoppingProducts = shoppingService.findOffersEnhanced(product, username, password, progressCallback);

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
                    
                    // Log summary if no matches found
                    if (offers.isEmpty()) {                        logger.warn("NO MATCHES FOUND - All {} offers were rejected. Consider lowering matching threshold.", shoppingProducts.size());
                        logger.info("Top rejected scores: Check logs above for specific scores and reasons");
                    }
                    
                    List<RetailerInfo> allOffers = new ArrayList<>();
                    if (product.getRetailers() != null) {
                        allOffers.addAll(product.getRetailers());
                        logger.info("Added {} existing offers from database", product.getRetailers().size());
                    }
                    allOffers.addAll(offers);                    // Enhanced deduplication - First group by retailer, then by URL
                    logger.info("DEDUPLICATION PHASE - Processing {} total offers", allOffers.size());
                    
                    // Step 1: Remove exact duplicates first (same retailer + same URL)
                    Set<String> seenKeys = new HashSet<>();
                    List<RetailerInfo> uniqueOffers = allOffers.stream()
                        .filter(offer -> offer.getName() != null && offer.getProductUrl() != null && !offer.getProductUrl().isEmpty())
                        .filter(offer -> {
                            String normalizedRetailer = normalizeRetailerName(offer.getName());
                            String normalizedUrl = normalizeUrl(offer.getProductUrl());
                            String uniqueKey = normalizedRetailer + "|" + normalizedUrl;
                            
                            if (seenKeys.contains(uniqueKey)) {                                logger.debug("DUPLICATE DETECTED - Removing duplicate: {} | {}", 
                                    normalizedRetailer, normalizedUrl);
                                return false;
                            }
                            seenKeys.add(uniqueKey);
                            return true;
                        })
                        .collect(Collectors.toList());
                    
                    logger.info("Generated {} unique keys for deduplication", seenKeys.size());
                    if (allOffers.size() > uniqueOffers.size()) {
                        logger.info("No duplicates detected - all offers are unique");
                    } else {
                        logger.info("Removed {} exact duplicate offers", allOffers.size() - uniqueOffers.size());
                    }
                    
                    // Step 2: Group by retailer name to limit one offer per retailer
                    java.util.Map<String, java.util.List<RetailerInfo>> offersByRetailer = uniqueOffers.stream()
                        .collect(Collectors.groupingBy(offer -> normalizeRetailerName(offer.getName())));
                      // Step 3: For each retailer, keep only the best offer (lowest price)
                    java.util.Map<String, RetailerInfo> bestOffersByRetailer = new java.util.HashMap<>();
                    for (java.util.Map.Entry<String, java.util.List<RetailerInfo>> entry : offersByRetailer.entrySet()) {
                        String retailerName = entry.getKey();
                        java.util.List<RetailerInfo> retailerOffers = entry.getValue();
                        
                        if (retailerOffers.size() > 1) {                            logger.info("RETAILER DEDUP - {} has {} offers, selecting best price", 
                                retailerName, retailerOffers.size());
                        }
                        
                        // Find the offer with the lowest price for this retailer
                        RetailerInfo bestOffer = retailerOffers.stream()
                            .min(java.util.Comparator.comparingDouble(RetailerInfo::getCurrentPrice))
                            .orElse(retailerOffers.get(0));
                        
                        bestOffersByRetailer.put(retailerName, bestOffer);
                        
                        if (retailerOffers.size() > 1) {                            logger.info("Selected best offer from {} with price: {}", 
                                retailerName, String.format("%.2f", bestOffer.getCurrentPrice()));
                        }
                    }
                    
                    logger.info("RETAILER DEDUPLICATION COMPLETE - {} unique retailers remain", bestOffersByRetailer.size());
                    
                    // Step 4: Additional URL-based deduplication within same retailer (if needed)
                    java.util.Map<String, RetailerInfo> finalOffers = new java.util.HashMap<>();
                    for (RetailerInfo offer : bestOffersByRetailer.values()) {
                        String normalizedRetailer = normalizeRetailerName(offer.getName());
                        String normalizedUrl = normalizeUrl(offer.getProductUrl());
                        String uniqueKey = normalizedRetailer + "|" + normalizedUrl;
                        
                        // If we already have an offer for this exact retailer+URL combination, keep the cheaper one
                        if (finalOffers.containsKey(uniqueKey)) {
                            RetailerInfo existing = finalOffers.get(uniqueKey);
                            if (offer.getCurrentPrice() < existing.getCurrentPrice()) {
                                finalOffers.put(uniqueKey, offer);
                                logger.info("URL DEDUP - Replaced existing offer with better price for {}", normalizedRetailer);
                            }
                        } else {
                            finalOffers.put(uniqueKey, offer);
                        }
                    }
                    logger.info("DEDUPLICATION COMPLETE - {} unique offers remain", finalOffers.size());
                    
                    List<RetailerInfo> sortedFinalOffers = finalOffers.values().stream()
                        .sorted(java.util.Comparator.comparingDouble(RetailerInfo::getCurrentPrice))
                        .limit(5)
                        .collect(Collectors.toList());
                    
                    // Final results
                    logger.info("FINAL RESULTS - Saving {} offers:", sortedFinalOffers.size());
                    sortedFinalOffers.forEach((offer) -> {
                        String shortUrl = offer.getProductUrl().length() > 40 ?
                                offer.getProductUrl().substring(0, 37) + "..." : offer.getProductUrl();
                        logger.info("  {} | Price: {} | URL: {}",
                            offer.getName(), String.format("%.2f", offer.getCurrentPrice()), shortUrl);
                    });
                    
                    product.setRetailers(sortedFinalOffers);
                    logger.info("Successfully saved {} offers to database", sortedFinalOffers.size());
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
    }    public List<ProductDocument> searchProducts(String query) {
        logger.info("Searching products with query: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return getAllProducts();
        }

        // First, search in Firebase with enhanced relevance scoring
        List<ProductDocument> localResults = firebaseService.searchProductsByName(query).join();
        if (!localResults.isEmpty()) {
            logger.info("Found {} products in Firebase for query: {}", localResults.size(), query);
            
            // Apply relevance scoring and filtering
            List<ProductDocument> relevantResults = filterAndRankByRelevance(localResults, query);
            logger.info("Filtered to {} relevant products after relevance scoring", relevantResults.size());
            
            relevantResults.forEach(this::retainOnlyAmazonRetailer);
            return relevantResults;
        }

        // If not found in Firebase, then search Amazon
        logger.info("No products found in Firebase for query: {}. Searching Amazon...", query);
        List<ProductDocument> searchResults = amazonApiService.searchProducts(query).join();
        searchResults.forEach(this::saveProduct);
        return searchResults;
    }
      /**
     * Enhanced search algorithm that filters and ranks products by relevance
     */
    private List<ProductDocument> filterAndRankByRelevance(List<ProductDocument> products, String query) {
        String normalizedQuery = query.toLowerCase().trim();
        String[] queryTerms = normalizedQuery.split("\\s+");
        
        List<ProductWithScore> scoredProducts = products.stream()
            .map(product -> {
                double relevanceScore = calculateRelevanceScore(product, normalizedQuery, queryTerms);
                return new ProductWithScore(product, relevanceScore);
            })
            .filter(pws -> pws.score >= 1.0) // Increased threshold from 0.3 to 1.0 for better filtering
            .sorted((a, b) -> Double.compare(b.score, a.score)) // Sort by relevance score descending
            .collect(Collectors.toList());
        
        // Log scoring information for debugging
        logger.info("RELEVANCE SCORING RESULTS:");
        logger.info("  Input products: {}", products.size());
        logger.info("  Products above threshold (1.0): {}", scoredProducts.size());
        
        if (scoredProducts.size() > 0) {
            int logCount = Math.min(5, scoredProducts.size());
            logger.info("  Top {} scored products:", logCount);
            for (int i = 0; i < logCount; i++) {
                ProductWithScore pws = scoredProducts.get(i);
                String shortName = pws.product.getName().length() > 50 ? 
                    pws.product.getName().substring(0, 47) + "..." : pws.product.getName();
                logger.info("    {}. Score: {} - {}", i + 1, String.format("%.2f", pws.score), shortName);
            }
        }
        
        return scoredProducts.stream()
            .limit(50) // Limit results to top 50 most relevant
            .map(pws -> pws.product)
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate relevance score for a product based on query terms
     */
    private double calculateRelevanceScore(ProductDocument product, String normalizedQuery, String[] queryTerms) {        String productName = product.getName().toLowerCase();
        String productDescription = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
        String productBrand = product.getBrand() != null ? product.getBrand().toLowerCase() : "";
        
        double score = 0.0;
        
        // 1. Exact query match in product name (highest weight)
        if (productName.contains(normalizedQuery)) {
            if (productName.startsWith(normalizedQuery)) {
                score += 15.0; // Product name starts with query - increased
            } else if (isMainProduct(productName, normalizedQuery)) {
                score += 12.0; // Query appears as main product (not accessory) - increased
            } else {
                score += 5.0; // Query appears somewhere in name - increased
            }
        }
        
        // 2. Main product vs accessory detection (crucial for iPhone search)
        if (isMainCategoryProduct(productName, normalizedQuery)) {
            score += 10.0; // Boost main category products significantly
        } else if (isAccessoryProduct(productName, productDescription)) {
            score -= 8.0; // Penalize accessories heavily
        }
        
        // 3. Individual term matching with position weighting
        for (String term : queryTerms) {
            if (term.length() < 2) continue; // Skip very short terms
            
            // Brand matching (high priority)
            if (productBrand.contains(term)) {
                score += 5.0;
            }
            
            // Product name term matching
            if (productName.contains(term)) {
                int position = productName.indexOf(term);
                if (position == 0) {
                    score += 4.0; // Term at beginning
                } else if (position < productName.length() / 3) {
                    score += 3.0; // Term in first third
                } else {
                    score += 1.5; // Term elsewhere
                }
            }
            
            // Description matching (lower priority)
            if (productDescription.contains(term)) {
                score += 1.0;
            }
        }
        
        // 4. Additional penalties for non-relevant products
        if (productName.length() > 100) {
            score -= 3.0; // Penalty for overly long names (often accessories)
        }
        
        String[] productWords = productName.split("\\W+");
        if (productWords.length > 15) {
            score -= 2.0; // Penalty for products with many words
        }
        
        // 5. Boost for high-rated products (slight preference)
        if (product.getRating() > 4.0) {
            score += 1.0;
        }
        
        return Math.max(0.0, score);
    }
      /**
     * Check if the product is likely the main product category being searched for
     */
    private boolean isMainProduct(String productName, String query) {
        // Check if query appears as a standalone word/model, not just mentioned
        String[] productWords = productName.split("\\W+");
        String[] queryWords = query.split("\\W+");
        
        // Look for consecutive matching words from query in product name
        for (int i = 0; i < productWords.length - queryWords.length + 1; i++) {
            boolean matches = true;
            for (int j = 0; j < queryWords.length; j++) {
                if (!productWords[i + j].equals(queryWords[j])) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }
      /**
     * Check if product is likely an accessory or peripheral item
     */
    private boolean isAccessoryProduct(String productName, String description) {
        String[] accessoryKeywords = {
            "adapter", "cable", "charger", "case", "cover", "stand", "mount", "holder",
            "screen protector", "tempered glass", "cable", "cord", "usb", "lightning",
            "wireless charger", "power bank", "car charger", "wall charger", "dock",
            "headphones", "earphones", "speaker", "bluetooth", "airpods case",
            "repair tool", "screwdriver", "kit", "tool set", "cleaning", "cleaner",
            "stylus", "pen", "grip", "ring holder", "car mount", "dashboard",
            "lens", "filter", "tripod", "selfie stick", "gimbal", "stabilizer",
            "converter", "splitter", "hub", "extension", "extender", "organizer",
            "protector", "wallet", "folio", "sleeve", "pouch", "bag", "carrying",
            "magnetic", "magsafe", "wireless pad", "charging pad", "charging station",
            "car adapter", "cigarette lighter", "retractable", "flexible shaft",
            "extension rod", "precision", "electronics repair", "magnetic", "bits",
            "female to", "male adapter", "type c", "converter", "plug", "power",
            "in one", "4 in one", "multi", "combo", "set of", "pack of", "pcs",
            "pieces", "replacement", "spare", "backup", "extra"
        };
        
        String combinedText = (productName + " " + description).toLowerCase();
        
        // Check for accessory keywords
        for (String keyword : accessoryKeywords) {
            if (combinedText.contains(keyword)) {
                return true;
            }
        }
        
        // Additional patterns for accessories
        if (combinedText.matches(".*\\d+\\s*(pcs?|pieces?).*") ||  // "2 pcs", "5 pieces"
            combinedText.matches(".*\\d+\\s*in\\s*one.*") ||       // "4 in one"
            combinedText.matches(".*set\\s+of\\s+\\d+.*") ||       // "set of 10"
            combinedText.contains("compatible with") ||
            combinedText.contains("for iphone") ||
            combinedText.contains("for apple") ||
            combinedText.contains("for samsung")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if product belongs to the main category being searched
     */
    private boolean isMainCategoryProduct(String productName, String query) {
        // Define main product categories and their indicators
        if (query.contains("iphone")) {
            return productName.matches(".*\\biphone\\s+\\d+.*") || 
                   productName.matches(".*\\biphone\\s+(pro|mini|plus|max).*");
        }
        
        if (query.contains("samsung") || query.contains("galaxy")) {
            return productName.matches(".*\\bgalaxy\\s+\\w+\\d+.*") ||
                   productName.matches(".*\\bsamsung\\s+galaxy.*");
        }
        
        if (query.contains("macbook")) {
            return productName.matches(".*\\bmacbook\\s+(air|pro).*");
        }
        
        if (query.contains("ipad")) {
            return productName.matches(".*\\bipad\\s+(air|pro|mini)?.*");
        }
        
        return false;
    }
    
    /**
     * Helper class to store product with its relevance score
     */
    private static class ProductWithScore {
        final ProductDocument product;
        final double score;
        
        ProductWithScore(ProductDocument product, double score) {
            this.product = product;
            this.score = score;
        }
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
        }        String scrapedDetails = scrapedDetailsBuilder.toString();
        
        String originalText = originalDetails.toString().toLowerCase();
        String scrapedText = scrapedDetails.toLowerCase();
        
        // Enhanced matching approach without image similarity
        double totalScore = 0.0;
        double maxScore = 0.0;
        
        // 1. Core text similarity (40% weight) - Increased from 35%
        org.apache.commons.text.similarity.JaroWinklerSimilarity jaroWinkler = new org.apache.commons.text.similarity.JaroWinklerSimilarity();
        double jaroWinklerScore = jaroWinkler.apply(originalText, scrapedText);
        totalScore += jaroWinklerScore * 0.40;
        maxScore += 0.40;
        
        // 2. Brand matching (25% weight) - Increased from 20%
        double brandScore = calculateBrandScore(originalProduct, scrapedText);
        totalScore += brandScore * 0.25;
        maxScore += 0.25;
        
        // 3. Specification-based matching (20% weight) - Increased from 12%
        double specScore = calculateSpecificationScore(originalProduct, scrapedProduct);
        totalScore += specScore * 0.20;
        maxScore += 0.20;
          // 4. Key terms overlap (15% weight) - Increased from 8%
        double keyTermScore = calculateKeyTermScore(originalText, scrapedText);
        totalScore += keyTermScore * 0.15;
        maxScore += 0.15;
        
        double finalScore = maxScore > 0 ? totalScore / maxScore : 0.0;
        
        // Dynamic threshold based on product type
        double threshold = getDynamicMatchingThreshold(originalProduct, scrapedProduct);
        boolean matches = finalScore > threshold;
        
        // Clean structured logging for product matching
        String result = matches ? "ACCEPTED" : "REJECTED";
        
        logger.debug("PRODUCT MATCH: {} | Score: {} | Original: {} | Scraped: {}",
            result,
            String.format("%.3f", finalScore),
            originalDetails.toString().length() > 40 ?
                originalDetails.toString().substring(0, 37) + "..." : originalDetails.toString(),
            scrapedProduct.getTitle().length() > 40 ?
                scrapedProduct.getTitle().substring(0, 37) + "..." : scrapedProduct.getTitle());        // Only log detailed breakdown for matches or close misses
        if (matches || finalScore > 0.5) {
            logger.info("MATCH ANALYSIS - {} (Score: {}) | {}", 
                result, 
                String.format("%.3f", finalScore),
                matches ? "ACCEPTED" : "REJECTED");
            logger.info("  Breakdown - Text: {} | Brand: {} | Specs: {} | Terms: {} | Product: {}",
                String.format("%.3f", jaroWinklerScore),
                String.format("%.3f", brandScore),
                String.format("%.3f", specScore),
                String.format("%.3f", keyTermScore),
                scrapedProduct.getTitle().length() > 50 ?
                    scrapedProduct.getTitle().substring(0, 47) + "..." : scrapedProduct.getTitle());
        }
        
        return matches;
    }    /**
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
        
        // Enhanced brand matching for different product categories
        String productCategory = detectProductCategory(originalProduct);
        
        // For books, try to match publisher or author names from title
        if ("books".equals(productCategory)) {
            String[] titleWords = originalProduct.getName().split("\\s+");
            // Check if any significant word from title appears in scraped text
            for (String word : titleWords) {
                if (word.length() > 4 && !isCommonStopWord(word.toLowerCase()) && 
                    scrapedText.contains(word.toLowerCase())) {
                    return 0.8; // Good match for book title elements
                }
            }
        }
        
        // For general products, try partial brand matching
        if (brand != null && brand.length() > 3) {
            // Check for partial brand match (first 3-4 characters)
            String partialBrand = brand.substring(0, Math.min(4, brand.length()));
            if (scrapedText.contains(partialBrand)) {
                return 0.6; // Partial brand match
            }
        }
        
        // For renewed products, also check for the brand without "renewed" context
        if (originalProduct.getName() != null && 
            (originalProduct.getName().toLowerCase().contains("renewed") || 
             originalProduct.getName().toLowerCase().contains("refurbished"))) {
            
            // Extract brand from the beginning of the product name
            String[] words = originalProduct.getName().split("\\s+");
            if (words.length > 0) {
                String firstWord = words[0].toLowerCase();
                if (scrapedText.contains(firstWord)) {
                    return 1.0;
                }
            }
        }
        
        return 0.0;
    }/**
     * Enhanced specification-based matching with better variant handling
     * Now handles color variants, model variations, and loose matching for better results
     */
    private double calculateSpecificationScore(ProductDocument originalProduct, ShoppingProduct scrapedProduct) {
        // Always try attribute matching first as primary method
        double attributeScore = calculateAttributeScore(originalProduct, scrapedProduct);
        
        // If we have detailed specifications, try to improve the score
        if (originalProduct.getSpecifications() != null && !originalProduct.getSpecifications().isEmpty()) {
            int matchingSpecs = 0;
            int totalSpecs = 0;
            int criticalMatches = 0; // For important specs like brand, model, size
            int totalCriticalSpecs = 0;
            int looseMatches = 0; // For partial/variant matches
            
            logger.debug("Checking {} specifications for product: {}", 
                originalProduct.getSpecifications().size(),
                originalProduct.getName().length() > 30 ? originalProduct.getName().substring(0, 27) + "..." : originalProduct.getName());
            
            // Build scraped text for fallback matching
            String scrapedText = (scrapedProduct.getTitle() + " " +
                                (scrapedProduct.getDescription() != null ? scrapedProduct.getDescription() : "")).toLowerCase();
            
            // Compare specifications when available
            for (com.example.price_comparator.model.SpecificationInfo originalSpec : originalProduct.getSpecifications()) {
                totalSpecs++;
                String specName = originalSpec.getName().toLowerCase();
                String specValue = originalSpec.getValue().toLowerCase();
                
                // Identify critical specifications for better matching
                boolean isCriticalSpec = isCriticalSpecification(specName);
                if (isCriticalSpec) {
                    totalCriticalSpecs++;
                }
                
                boolean foundMatch = false;
                
                // Strategy 1: Check structured specifications if available
                if (scrapedProduct.getSpecifications() != null && !scrapedProduct.getSpecifications().isEmpty()) {
                    for (com.example.price_comparator.model.SpecificationInfo scrapedSpec : scrapedProduct.getSpecifications()) {
                        String scrapedSpecName = scrapedSpec.getName().toLowerCase();
                        String scrapedSpecValue = scrapedSpec.getValue().toLowerCase();
                        
                        if (areSpecificationNamesRelated(specName, scrapedSpecName)) {
                            // Exact match
                            if (areSpecValuesCompatible(specValue, scrapedSpecValue)) {
                                matchingSpecs++;
                                if (isCriticalSpec) criticalMatches++;
                                foundMatch = true;
                                logger.debug("✓ EXACT spec match: {} = '{}' vs '{}'", specName, specValue, scrapedSpecValue);
                                break;
                            }
                            // Variant match (for colors, models, etc.)
                            else if (areSpecValuesVariants(specValue, scrapedSpecValue, specName)) {
                                looseMatches++;
                                if (isCriticalSpec) criticalMatches++;
                                foundMatch = true;
                                logger.debug("✓ VARIANT spec match: {} = '{}' vs '{}' (variant)", specName, specValue, scrapedSpecValue);
                                break;
                            }
                        }
                    }
                }
                
                // Strategy 2: Check in product text if no structured match found
                if (!foundMatch) {
                    if (scrapedText.contains(specValue)) {
                        looseMatches++;
                        if (isCriticalSpec) criticalMatches++;
                        foundMatch = true;
                        logger.debug("✓ TEXT spec match: {} = '{}' found in text", specName, specValue);
                    }
                    // For colors, check for partial matches (e.g., "blue" matches "navy blue")
                    else if (specName.contains("color") || specName.contains("colour")) {
                        if (isColorVariant(specValue, scrapedText)) {
                            looseMatches++;
                            if (isCriticalSpec) criticalMatches++;
                            foundMatch = true;
                            logger.debug("✓ COLOR variant match: '{}' found as color variant in text", specValue);
                        }
                    }
                }
                
                if (!foundMatch) {
                    logger.debug("✗ NO match for: {} = '{}'", specName, specValue);
                }
            }
            
            if (totalSpecs > 0) {
                // Calculate combined score with both exact and loose matches
                double exactScore = (double) matchingSpecs / totalSpecs;
                double looseScore = (double) looseMatches / totalSpecs;
                double combinedSpecScore = (exactScore * 0.8) + (looseScore * 0.4); // Weight exact matches more
                
                // Boost score if critical specifications match well
                if (totalCriticalSpecs > 0) {
                    double criticalScore = (double) criticalMatches / totalCriticalSpecs;
                    combinedSpecScore = (combinedSpecScore * 0.7) + (criticalScore * 0.3); // Weight critical specs more
                    logger.debug("Critical spec score: {}/{} = {:.3f} ({:.0f}%)", 
                        criticalMatches, totalCriticalSpecs, criticalScore, criticalScore * 100);
                }
                
                logger.debug("Enhanced spec score: exact={}/{}, loose={}/{}, combined={:.3f} ({:.0f}%)", 
                    matchingSpecs, totalSpecs, looseMatches, totalSpecs, combinedSpecScore, combinedSpecScore * 100);
                
                // Use the better of attribute score or detailed spec score
                return Math.max(attributeScore, combinedSpecScore);
            }
        } else {
            logger.debug("No detailed specifications available, using attribute score: {:.3f}", attributeScore);
        }
        
        return attributeScore;
    }
    
    /**
     * Check if a specification name indicates a critical product attribute
     */
    private boolean isCriticalSpecification(String specName) {
        String[] criticalSpecs = {
            "brand", "model", "size", "color", "material", "type", 
            "display size", "storage", "ram", "processor", "camera",
            "battery", "weight", "dimensions"
        };
        
        for (String critical : criticalSpecs) {
            if (specName.contains(critical) || critical.contains(specName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if two specification names are related (handles variations in naming)
     */
    private boolean areSpecificationNamesRelated(String name1, String name2) {
        // Direct match
        if (name1.equals(name2)) return true;
        
        // Check if either contains the other
        if (name1.contains(name2) || name2.contains(name1)) return true;
        
        // Check for common variations
        String[][] synonyms = {
            {"color", "colour"},
            {"size", "dimensions"},
            {"weight", "mass"},
            {"material", "made of", "construction"},
            {"display", "screen"},
            {"storage", "capacity", "memory"},
            {"battery", "power", "mah"},
            {"processor", "cpu", "chip"},
            {"camera", "megapixel", "mp"},
            {"connectivity", "wireless", "bluetooth", "wifi"}
        };
        
        for (String[] group : synonyms) {
            boolean name1InGroup = false;
            boolean name2InGroup = false;
            
            for (String synonym : group) {
                if (name1.contains(synonym)) name1InGroup = true;
                if (name2.contains(synonym)) name2InGroup = true;
            }
            
            if (name1InGroup && name2InGroup) return true;
        }
        
        return false;
    }    /**
     * Enhanced fallback attribute matching for products without detailed specifications
     */
    private double calculateAttributeScore(ProductDocument originalProduct, ShoppingProduct scrapedProduct) {
        double score = 0.0;
        int attributes = 0;
        int matches = 0;
        
        String scrapedText = (scrapedProduct.getTitle() + " " +
                             (scrapedProduct.getDescription() != null ? scrapedProduct.getDescription() : "")).toLowerCase();
        
        String originalText = (originalProduct.getName() + " " +
                              (originalProduct.getDescription() != null ? originalProduct.getDescription() : "")).toLowerCase();
        
        // Detect product category for adaptive matching
        String productCategory = detectProductCategory(originalProduct);
        boolean isElectronic = productCategory.equals("electronics");
        
        // Enhanced color matching with variants
        if (originalProduct.getColor() != null && !originalProduct.getColor().trim().isEmpty()) {
            attributes++;
            String originalColor = originalProduct.getColor().toLowerCase().trim();
            
            // Exact match
            if (scrapedText.contains(originalColor)) {
                score += 1.0;
                matches++;
                logger.debug("✓ EXACT color match found: {}", originalColor);
            }
            // Variant match
            else if (isColorVariant(originalColor, scrapedText)) {
                score += 0.8; // Slightly lower score for variant
                matches++;
                logger.debug("✓ COLOR variant match found: {} (variant in text)", originalColor);
            }
            // Base color match (e.g., "blue" in "navy blue")
            else if (hasBaseColorMatch(originalColor, scrapedText)) {
                score += 0.6;
                matches++;
                logger.debug("✓ BASE color match found: {} (base color in text)", originalColor);
            }
            else {
                logger.debug("✗ NO color match for: {}", originalColor);
            }
        }
        
        // Electronic-specific attributes (only for electronic products)
        if (isElectronic) {
            // Enhanced storage matching with normalization
            if (originalProduct.getStorage() != null && !originalProduct.getStorage().trim().isEmpty()) {
                attributes++;
                String originalStorage = originalProduct.getStorage().toLowerCase().trim();
                String normalizedStorage = originalStorage.replaceAll("\\s+", "");
                
                if (scrapedText.contains(originalStorage) || scrapedText.replaceAll("\\s+", "").contains(normalizedStorage)) {
                    score += 1.0;
                    matches++;
                    logger.debug("✓ STORAGE match found: {}", originalStorage);
                } else {
                    logger.debug("✗ NO storage match for: {}", originalStorage);
                }
            }

            // Enhanced RAM matching with normalization
            if (originalProduct.getRam() != null && !originalProduct.getRam().trim().isEmpty()) {
                attributes++;
                String originalRam = originalProduct.getRam().toLowerCase().trim();
                String normalizedRam = originalRam.replaceAll("\\s+", "");
                
                if (scrapedText.contains(originalRam) || scrapedText.replaceAll("\\s+", "").contains(normalizedRam)) {
                    score += 1.0;
                    matches++;
                    logger.debug("✓ RAM match found: {}", originalRam);
                } else {
                    logger.debug("✗ NO RAM match for: {}", originalRam);
                }
            }

            // Enhanced model matching with partial matching
            if (originalProduct.getModel() != null && !originalProduct.getModel().trim().isEmpty()) {
                attributes++;
                String originalModel = originalProduct.getModel().toLowerCase().trim();
                
                if (scrapedText.contains(originalModel)) {
                    score += 1.0;
                    matches++;
                    logger.debug("✓ EXACT model match found: {}", originalModel);
                } else if (areModelVariants(originalModel, scrapedText)) {
                    score += 0.8;
                    matches++;
                    logger.debug("✓ MODEL variant match found: {}", originalModel);
                } else {
                    logger.debug("✗ NO model match for: {}", originalModel);
                }
            }
        }
          // Universal attribute matching for all product types
        // Extract and match important keywords from product names/descriptions
        String[] originalKeywords = extractCategorySpecificKeywords(originalText, productCategory);
        
        for (String keyword : originalKeywords) {
            if (keyword.length() > 2 && scrapedText.contains(keyword)) {
                attributes++;
                score += 0.7; // Good weight for keyword matches
                matches++;
                logger.debug("✓ KEYWORD match found: {}", keyword);
            }
        }
        
        // Enhanced brand matching with fuzzy logic
        String originalBrand = extractUniversalBrand(originalProduct.getName());  
        if (originalBrand != null && !originalBrand.isEmpty()) {
            attributes++;
            if (scrapedText.contains(originalBrand.toLowerCase())) {
                score += 1.2; // High weight for brand matches
                matches++;
                logger.debug("✓ BRAND match found: {}", originalBrand);
            } else {
                logger.debug("✗ NO brand match for: {}", originalBrand);
            }
        }
        
        // Size/dimension matching for applicable products
        String[] sizePatterns = extractSizePatterns(originalText);
        for (String sizePattern : sizePatterns) {
            if (!sizePattern.isEmpty() && scrapedText.contains(sizePattern.toLowerCase())) {
                attributes++;
                score += 0.8;
                matches++;
                logger.debug("✓ SIZE pattern match found: {}", sizePattern);
            }
        }
        
        double finalScore;
        if (attributes > 0) {
            finalScore = score / attributes;
        } else {
            // If no specific attributes found, give a base score for general products
            // This is crucial for products without structured attributes
            finalScore = calculateFallbackTextSimilarity(originalText, scrapedText);
        }
        
        logger.debug("Attribute matching: {}/{} matches, {} total attributes, final score: {} (category: {})", 
            matches, attributes, attributes, String.format("%.3f", finalScore), productCategory);
        
        return Math.min(1.0, finalScore); // Cap at 1.0
    }
      /**
     * Extract category-specific keywords for better matching
     */
    private String[] extractCategorySpecificKeywords(String text, String category) {
        Set<String> keywords = new HashSet<>();
        String[] words = text.toLowerCase().split("\\W+");
        
        switch (category.toLowerCase()) {
            case "books":
                // For books, focus on title words, author-like terms, and book-specific terms
                for (String word : words) {
                    if (word.length() > 3 && !isCommonStopWord(word)) {
                        // Include meaningful words from book titles
                        if (word.matches("^[a-z]+$") && word.length() > 4) {
                            keywords.add(word);
                        }
                    }
                }
                break;
                
            case "home":
            case "kitchen":
                // For home/kitchen, focus on material, size, and function keywords
                String[] homeKeywords = {"steel", "plastic", "wood", "glass", "ceramic", "aluminum", 
                                       "storage", "organizer", "container", "holder", "rack"};
                for (String homeKeyword : homeKeywords) {
                    if (text.contains(homeKeyword)) {
                        keywords.add(homeKeyword);
                    }
                }
                break;
                
            case "clothing":
                // For clothing, focus on size, material, and style keywords
                String[] clothingKeywords = {"cotton", "polyester", "silk", "wool", "denim", 
                                           "sleeve", "collar", "pocket", "zipper"};
                for (String clothingKeyword : clothingKeywords) {
                    if (text.contains(clothingKeyword)) {
                        keywords.add(clothingKeyword);
                    }
                }
                break;
                
            case "health":
            case "beauty":
                // For health/beauty, focus on ingredients and purpose keywords
                String[] healthKeywords = {"vitamin", "organic", "natural", "serum", "cream", 
                                         "lotion", "supplement", "tablets", "capsules"};
                for (String healthKeyword : healthKeywords) {
                    if (text.contains(healthKeyword)) {
                        keywords.add(healthKeyword);
                    }
                }
                break;
                
            default:
                // For general products, extract meaningful terms
                for (String word : words) {
                    if (word.length() > 3 && !isCommonStopWord(word)) {                        // Include numbers, long words, or brand-like terms
                        if (word.matches(".*\\d.*") || word.length() > 5) {
                            keywords.add(word);
                        }
                    }
                }
        }
        
        return keywords.stream().limit(8).toArray(String[]::new);
    }
    
    /**
     * Extract size/dimension patterns from text
     */
    private String[] extractSizePatterns(String text) {
        Set<String> patterns = new HashSet<>();
        
        // Look for dimension patterns (e.g., "12x8", "5 inch", "20cm")
        java.util.regex.Pattern dimensionPattern = java.util.regex.Pattern.compile(
            "\\b\\d+[x×]\\d+|\\d+\\s*(inch|inches|cm|mm|ft|feet|meter|metres?)\\b", 
            java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = dimensionPattern.matcher(text);
        
        while (matcher.find()) {
            patterns.add(matcher.group().toLowerCase());
        }
        
        // Look for common size indicators
        String[] sizeKeywords = {"small", "medium", "large", "xl", "xxl", "mini", "compact", "full", "queen", "king"};
        String lowerText = text.toLowerCase();
        for (String size : sizeKeywords) {
            if (lowerText.contains(size)) {
                patterns.add(size);
            }
        }
        
        return patterns.toArray(String[]::new);
    }
    
    /**
     * Calculate fallback text similarity for products without structured attributes
     */
    private double calculateFallbackTextSimilarity(String originalText, String scrapedText) {
        if (originalText == null || scrapedText == null || originalText.trim().isEmpty()) {
            return 0.3; // Base score for minimal data
        }
        
        // Use Jaro-Winkler similarity as base
        org.apache.commons.text.similarity.JaroWinklerSimilarity jaroWinkler = 
            new org.apache.commons.text.similarity.JaroWinklerSimilarity();
        double jaroScore = jaroWinkler.apply(originalText, scrapedText);
        
        // Boost score with common word overlap
        String[] originalWords = originalText.split("\\s+");
        String[] scrapedWords = scrapedText.split("\\s+");
        
        Set<String> originalSet = new HashSet<>(Arrays.asList(originalWords));
        Set<String> scrapedSet = new HashSet<>(Arrays.asList(scrapedWords));
        
        // Remove stop words
        originalSet.removeIf(this::isCommonStopWord);
        scrapedSet.removeIf(this::isCommonStopWord);
        
        // Calculate word overlap
        Set<String> intersection = new HashSet<>(originalSet);
        intersection.retainAll(scrapedSet);
        
        double wordOverlap = originalSet.isEmpty() ? 0.0 : (double) intersection.size() / originalSet.size();
        
        // Combine scores with weights
        double combinedScore = (jaroScore * 0.6) + (wordOverlap * 0.4);
        
        // Ensure minimum score for reasonable similarity
        return Math.max(0.3, combinedScore);
    }
    
    /**
     * Check if a word is a common stop word
     */
    private boolean isCommonStopWord(String word) {
        if (word == null || word.length() < 2) return true;
        
        Set<String> stopWords = Set.of("the", "and", "or", "but", "in", "on", "at", "to", "for", 
                                     "of", "with", "by", "is", "are", "was", "were", "been", "be", 
                                     "have", "has", "had", "do", "does", "did", "will", "would", 
                                     "could", "should", "may", "might", "can", "shall", "this", 
                                     "that", "these", "those", "from", "up", "down", "out", "off", 
                                     "over", "under", "again", "further", "then", "once");
        
        return stopWords.contains(word.toLowerCase());
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
                            .filter(java.util.Objects::nonNull)                            .collect(Collectors.toList()));
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
        
        // Handle Amazon variations more comprehensively
        if (normalized.contains("amazon")) {
            return "amazon";
        }
        
        // Handle other common retailer variations
        if (normalized.contains("noon")) {
            return "noon";
        }
        
        if (normalized.contains("lulu") || normalized.contains("luluhypermarket")) {
            return "lulu";
        }
        
        if (normalized.contains("carrefour")) {
            return "carrefour";
        }
        
        if (normalized.contains("sharaf") || normalized.contains("sharafdg")) {
            return "sharafdg";
        }
        
        if (normalized.contains("cartlow")) {
            return "cartlow";
        }
        
        // Remove common suffixes/prefixes
        normalized = normalized.replaceAll("\\.(ae|com|net|org)$", "");
        normalized = normalized.replaceAll("^(www\\.|m\\.|mobile\\.)", "");
        
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
            logger.debug("URL Fallback Normalization: {} -> {}",
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
                logger.debug("Amazon ASIN extracted: {} from /dp/ pattern", asin);
                return "/dp/" + asin;
            }
            
            // Alternative pattern for /gp/product/ URLs
            java.util.regex.Pattern altPattern = java.util.regex.Pattern.compile("/gp/product/([A-Za-z0-9]{10})", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher altMatcher = altPattern.matcher(lowerPath);
            if (altMatcher.find()) {
                String asin = altMatcher.group(1).toUpperCase(); // Normalize to uppercase
                logger.debug("Amazon ASIN extracted: {} from /gp/product/ pattern", asin);
                return "/dp/" + asin;
            }
            
            logger.warn("Could not extract ASIN from Amazon path: {}",
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
    private String extractBrandFromTitle(String title) {
        if (title == null || title.isEmpty()) {
            return null;
        }
        
        // Simple heuristic: Take the first capitalized word as brand
        String[] words = title.split("\\s+");
        for (String word : words) {
            if (Character.isUpperCase(word.charAt(0))) {
                return word;
            }
        }
        
        return null;
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
     * Check if two specification values are variants of each other
     * Handles cases like "Blue" vs "Navy Blue", "64GB" vs "64 GB", etc.
     */
    private boolean areSpecValuesVariants(String originalValue, String scrapedValue, String specName) {
        if (originalValue == null || scrapedValue == null) return false;
        
        String orig = originalValue.trim();
        String scraped = scrapedValue.trim();
        
        // Handle color variants
        if (specName.contains("color") || specName.contains("colour")) {
            return areColorVariants(orig, scraped);
        }
        
        // Handle size/storage variants (normalize spacing and units)
        if (specName.contains("size") || specName.contains("storage") || specName.contains("memory")) {
            String normalizedOrig = orig.replaceAll("\\s+", "").toLowerCase();
            String normalizedScraped = scraped.replaceAll("\\s+", "").toLowerCase();
            return normalizedOrig.equals(normalizedScraped) || 
                   normalizedOrig.contains(normalizedScraped) || 
                   normalizedScraped.contains(normalizedOrig);
        }
        
        // Handle model variants (partial matching)
        if (specName.contains("model")) {
            return orig.contains(scraped) || scraped.contains(orig) ||
                   areModelVariants(orig, scraped);
        }
        
        // General variant matching - check if one contains the other
        return orig.contains(scraped) || scraped.contains(orig);
    }
    
    /**
     * Check if two colors are variants (e.g., "Blue" and "Navy Blue")
     */
    private boolean areColorVariants(String color1, String color2) {
        String[] baseColors = {"red", "blue", "green", "yellow", "black", "white", "gray", "grey", 
                              "purple", "pink", "orange", "brown", "silver", "gold"};
        
        String c1 = color1.toLowerCase();
        String c2 = color2.toLowerCase();
        
        // Check if both colors contain the same base color
        for (String baseColor : baseColors) {
            if (c1.contains(baseColor) && c2.contains(baseColor)) {
                return true;
            }
        }
        
        // Check for common color synonyms
        String[][] colorSynonyms = {
            {"grey", "gray"},
            {"silver", "metallic"},
            {"gold", "golden"},
            {"black", "dark"},
            {"white", "light"}
        };
        
        for (String[] synonyms : colorSynonyms) {
            boolean c1HasSynonym = false;
            boolean c2HasSynonym = false;
            
            for (String synonym : synonyms) {
                if (c1.contains(synonym)) c1HasSynonym = true;
                if (c2.contains(synonym)) c2HasSynonym = true;
            }
            
            if (c1HasSynonym && c2HasSynonym) return true;
        }
        
        return false;
    }
    
    /**
     * Check if a color value appears as a variant in the text
     */
    private boolean isColorVariant(String colorValue, String text) {
        String color = colorValue.toLowerCase();
        String searchText = text.toLowerCase();
        
        // Direct match
        if (searchText.contains(color)) return true;
        
        // Check for color variants in text
        String[] baseColors = {"red", "blue", "green", "yellow", "black", "white", "gray", "grey", 
                              "purple", "pink", "orange", "brown", "silver", "gold"};
        
        for (String baseColor : baseColors) {
            if (color.contains(baseColor) && searchText.contains(baseColor)) {
                return true;
            }
        }
        
        return false;
    }
      /**
     * Check if two model values are variants or if a model appears in text
     */
    private boolean areModelVariants(String model1, String textOrModel2) {
        String m1 = model1.toLowerCase().replaceAll("[^a-z0-9]", "");
        String m2 = textOrModel2.toLowerCase().replaceAll("[^a-z0-9]", "");
        
        // Check if core model numbers match
        if (m1.length() >= 3 && m2.length() >= 3) {
            return m1.contains(m2.substring(0, Math.min(3, m2.length()))) ||
                   m2.contains(m1.substring(0, Math.min(3, m1.length()))) ||
                   m2.contains(m1) || m1.contains(m2);
        }
        
        return m2.contains(m1) || m1.contains(m2);
    }

    /**
     * Check if text contains a base color that matches the original color
     * E.g., "blue" matches text containing "navy blue" or "light blue"
     */
    private boolean hasBaseColorMatch(String originalColor, String text) {
        String[] baseColors = {"red", "blue", "green", "yellow", "black", "white", "gray", "grey", 
                              "purple", "pink", "orange", "brown", "silver", "gold"};
        
        String color = originalColor.toLowerCase();
        String searchText = text.toLowerCase();
        
        // Find which base color the original contains
        for (String baseColor : baseColors) {
            if (color.contains(baseColor)) {
                // Check if the text contains this base color
                if (searchText.contains(baseColor)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Dynamic threshold calculation based on product type and available attributes
     */
    private double getDynamicMatchingThreshold(ProductDocument originalProduct, ShoppingProduct scrapedProduct) {
        // Start with base threshold
        double baseThreshold = 0.45; // Lowered from 0.62 for better general product matching
        
        // Detect product category to adjust threshold
        String productCategory = detectProductCategory(originalProduct);
        
        switch (productCategory.toLowerCase()) {
            case "electronics":
            case "computers":
            case "smartphones":
                // Electronics can have higher threshold due to structured specs
                return 0.58;
                
            case "books":
                // Books often have generic titles, lower threshold
                return 0.35;
                
            case "home":
            case "kitchen":
            case "tools":
            case "automotive":
                // Home/kitchen items often have less structured data
                return 0.40;
                
            case "clothing":
            case "fashion":
                // Fashion items often have size/color variants
                return 0.42;
                
            case "health":
            case "beauty":
                // Beauty products often have brand/size focus
                return 0.38;
                
            default:
                // General products - most lenient threshold
                return baseThreshold;
        }
    }
    
    /**
     * Detect product category based on product name and attributes
     */
    private String detectProductCategory(ProductDocument product) {
        if (product == null || product.getName() == null) {
            return "general";
        }
        
        String name = product.getName().toLowerCase();
        String description = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
        String combined = name + " " + description;
        
        // Electronics indicators
        if (hasElectronicsKeywords(combined) || 
            product.getStorage() != null || product.getRam() != null ||
            product.getModel() != null) {
            return "electronics";
        }
        
        // Books indicators
        if (combined.contains("book") || combined.contains("novel") || 
            combined.contains("author") || combined.contains("isbn") ||
            combined.contains("paperback") || combined.contains("hardcover") ||
            combined.contains("kindle") || combined.contains("audiobook")) {
            return "books";
        }
        
        // Home/Kitchen indicators
        if (combined.matches(".*(kitchen|home|house|furniture|decor|storage|organizer|container|utensil|cookware|bedding|bathroom|cleaning).*")) {
            return "home";
        }
        
        // Clothing/Fashion indicators
        if (combined.matches(".*(shirt|pants|dress|shoes|clothing|fashion|apparel|wear|size|fit).*")) {
            return "clothing";
        }
        
        // Health/Beauty indicators  
        if (combined.matches(".*(beauty|cosmetic|skincare|health|vitamin|supplement|lotion|cream|shampoo).*")) {
            return "health";
        }
        
        // Tools/Automotive indicators
        if (combined.matches(".*(tool|repair|automotive|car|vehicle|mechanic|screwdriver|wrench|drill).*")) {
            return "tools";
        }
        
        return "general";
    }
    
    /**
     * Check if product has electronics-related keywords
     */
    private boolean hasElectronicsKeywords(String text) {
        String[] electronicsKeywords = {
            "iphone", "samsung", "laptop", "computer", "tablet", "phone", "smartphone",
            "tv", "monitor", "camera", "headphones", "speaker", "gaming", "console",
            "processor", "cpu", "gpu", "memory", "storage", "ssd", "hdd", "usb",
            "bluetooth", "wifi", "wireless", "charger", "battery", "electronic",
            "digital", "smart", "tech", "device", "gadget"
        };
        
        for (String keyword : electronicsKeywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
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
        
        for (String word : words) {
            if (word.length() > 2 && !isCommonStopWord(word)) {
                // Include numbers, brand-like words, and significant terms
                if (word.matches(".*\\d.*") || word.length() > 4) {
                    keyTerms.add(word);
                }
            }
        }
        
        return keyTerms;
    }
}
