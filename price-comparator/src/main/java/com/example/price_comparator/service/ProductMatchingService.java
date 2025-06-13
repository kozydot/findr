package com.example.price_comparator.service;

import com.example.price_comparator.dto.ComparisonResult;
import com.example.price_comparator.dto.*;
import com.example.price_comparator.model.AliexpressProduct;
import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.model.SpecificationInfo;
import com.example.price_comparator.dto.*;
import com.example.price_comparator.model.AliexpressProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.model.SpecificationInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductMatchingService {

    private static final Logger logger = LoggerFactory.getLogger(ProductMatchingService.class);
    private static final double WEIGHT_TITLE = 0.45;
    private static final double WEIGHT_IMAGE = 0.40;
    private static final double WEIGHT_SELLER = 0.15;
    private static final double MIN_CONFIDENCE_THRESHOLD = 0.70;

    private final AliexpressApiService aliexpressApiService;
    private final ExchangeRateService exchangeRateService;
    private final ImageHashingService imageHashingService;

    @Autowired
    public ProductMatchingService(AliexpressApiService aliexpressApiService, ExchangeRateService exchangeRateService, ImageHashingService imageHashingService) {
        this.aliexpressApiService = aliexpressApiService;
        this.exchangeRateService = exchangeRateService;
        this.imageHashingService = imageHashingService;
    }

    public ComparisonResult findAndCompare(ProductDocument amazonProduct) {
        logger.info("Starting comparison for Amazon product: {}", amazonProduct.getName());

        // Step 1: Smart Search Query Generation
        String brand = amazonProduct.getBrand();
        String modelNumber = extractSpec(amazonProduct, "Model Number");
        String searchTerm = (brand != null ? brand + " " + modelNumber : modelNumber).trim();
        if (searchTerm.isEmpty()) {
            searchTerm = extractSignificantKeywords(amazonProduct.getName());
        }
        logger.info("Generated search term: '{}'", searchTerm);
        String sanitizedQuery = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8);

        // Step 2: API Call and Initial Filtering
        logger.info("Querying AliExpress with: '{}'", sanitizedQuery);
        List<AliexpressProduct> aliexpressResults = aliexpressApiService.searchProducts(sanitizedQuery, "AE", "USD");
        if (aliexpressResults.isEmpty()) {
            logger.warn("API call to AliExpress failed or returned no results for query: {}", sanitizedQuery);
            return new ComparisonResult(false, "API call to AliExpress failed or returned no results.");
        }
        logger.info("Found {} initial results from AliExpress.", aliexpressResults.size());

        List<AliexpressProduct> filteredCandidates;
        if (brand != null && !brand.isEmpty()) {
            filteredCandidates = aliexpressResults.stream()
                .filter(p -> p.getProductTitle().toLowerCase().contains(brand.toLowerCase()))
                .collect(Collectors.toList());
            logger.info("Filtered to {} candidates based on brand: '{}'", filteredCandidates.size(), brand);
        } else {
            filteredCandidates = aliexpressResults;
        }

        if (filteredCandidates.isEmpty()) {
            logger.warn("No products found on AliExpress matching the brand '{}'", brand);
            return new ComparisonResult(false, "No products found on AliExpress matching the brand.");
        }

        // Step 3: Multi-Factor Match Scoring
        AliexpressProduct bestMatch = null;
        double highestScore = 0.0;
        long maxSaleVolume = filteredCandidates.stream().mapToLong(AliexpressProduct::getLatestSaleVolume).max().orElse(0);
        logger.info("Max sale volume in candidate set: {}", maxSaleVolume);

        for (AliexpressProduct candidate : filteredCandidates) {
            double titleScore = calculateTitleScore(amazonProduct.getName(), candidate.getProductTitle(), modelNumber);
            double imageScore = calculateImageScore(amazonProduct.getImageUrl(), candidate.getMainImageUrl());
            double sellerScore = (maxSaleVolume > 0) ? (double) candidate.getLatestSaleVolume() / maxSaleVolume : 0;

            double compositeScore = (titleScore * WEIGHT_TITLE) + (imageScore * WEIGHT_IMAGE) + (sellerScore * WEIGHT_SELLER);
            logger.debug("Candidate: '{}' | Title: {:.2f}, Image: {:.2f}, Seller: {:.2f} | Composite: {:.2f}",
                candidate.getProductTitle(), titleScore, imageScore, sellerScore, compositeScore);

            if (compositeScore > highestScore) {
                highestScore = compositeScore;
                bestMatch = candidate;
            }
        }

        // Step 4: Best Match Selection
        if (bestMatch == null || highestScore < MIN_CONFIDENCE_THRESHOLD) {
            logger.warn("No confident match found. Best score was {:.2f}, which is below the threshold of {}", highestScore, MIN_CONFIDENCE_THRESHOLD);
            return new ComparisonResult(false, "No confident match found on AliExpress for the given product.");
        }
        logger.info("Best match found: '{}' with score {:.2f}", bestMatch.getProductTitle(), highestScore);

        // Step 5: Final Price Comparison
        double aliexpressPriceUsd = bestMatch.getSalePrice();
        double exchangeRate = exchangeRateService.getUsdToAedRate();
        double aliexpressPriceAed = aliexpressPriceUsd * exchangeRate;
        double amazonPriceAed = Double.parseDouble(amazonProduct.getPrice().replaceAll("[^\\d.]", ""));
        double priceDifference = amazonPriceAed - aliexpressPriceAed;
        String cheaperStore = priceDifference < 0 ? "Amazon" : "AliExpress";

        AmazonProduct amazonDto = new AmazonProduct(amazonProduct.getName(), amazonPriceAed);
        AliexpressMatch aliexpressDto = new AliexpressMatch(bestMatch.getProductTitle(), bestMatch.getProductDetailUrl(), bestMatch.getSalePrice(), bestMatch.getShopName());
        PriceComparison priceDto = new PriceComparison(amazonPriceAed, aliexpressPriceAed, Math.abs(priceDifference), cheaperStore, "Shipping costs not included. Verify final price on seller's page.");

        return new ComparisonResult(true, amazonDto, aliexpressDto, highestScore, priceDto);
    }

    private String extractSpec(ProductDocument product, String key) {
        if (product.getSpecifications() == null) return "";
        return product.getSpecifications().stream()
                .filter(spec -> key.equalsIgnoreCase(spec.getName()))
                .map(SpecificationInfo::getValue)
                .findFirst()
                .orElse("");
    }

    private String extractSignificantKeywords(String title) {
        // Simple implementation, a more robust solution would use NLP
        return Arrays.stream(title.split("\\s+")).limit(4).collect(Collectors.joining(" "));
    }

    private double calculateTitleScore(String title1, String title2, String modelNumber) {
        Set<String> set1 = normalizeAndTokenize(title1);
        Set<String> set2 = normalizeAndTokenize(title2);

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        double jaccardScore = (double) intersection.size() / union.size();

        if (!modelNumber.isEmpty() && title2.toLowerCase().contains(modelNumber.toLowerCase())) {
            jaccardScore *= 1.20;
        }

        return Math.min(1.0, jaccardScore);
    }

    private Set<String> normalizeAndTokenize(String text) {
        // Simple normalization
        String[] tokens = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        return new HashSet<>(Arrays.asList(tokens));
    }

    private double calculateImageScore(String imageUrl1, String imageUrl2) {
        String pHash1 = imageHashingService.getPHash(imageUrl1);
        String pHash2 = imageHashingService.getPHash(imageUrl2);
        int hammingDistance = imageHashingService.calculateHammingDistance(pHash1, pHash2);
        return (64.0 - hammingDistance) / 64.0;
    }
}