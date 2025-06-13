package com.example.price_comparator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComparisonResult {

    private boolean matchFound;
    private String message;
    private AmazonProduct amazonProduct;
    private AliexpressMatch aliexpressMatch;
    private Double matchScore;
    private PriceComparison priceComparison;

    // Constructor for no match found
    public ComparisonResult(boolean matchFound, String message) {
        this.matchFound = matchFound;
        this.message = message;
    }

    // Constructor for a successful match
    public ComparisonResult(boolean matchFound, AmazonProduct amazonProduct, AliexpressMatch aliexpressMatch, Double matchScore, PriceComparison priceComparison) {
        this.matchFound = matchFound;
        this.amazonProduct = amazonProduct;
        this.aliexpressMatch = aliexpressMatch;
        this.matchScore = matchScore;
        this.priceComparison = priceComparison;
    }

    // Getters and Setters
    public boolean isMatchFound() {
        return matchFound;
    }

    public void setMatchFound(boolean matchFound) {
        this.matchFound = matchFound;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AmazonProduct getAmazonProduct() {
        return amazonProduct;
    }

    public void setAmazonProduct(AmazonProduct amazonProduct) {
        this.amazonProduct = amazonProduct;
    }

    public AliexpressMatch getAliexpressMatch() {
        return aliexpressMatch;
    }

    public void setAliexpressMatch(AliexpressMatch aliexpressMatch) {
        this.aliexpressMatch = aliexpressMatch;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }

    public PriceComparison getPriceComparison() {
        return priceComparison;
    }

    public void setPriceComparison(PriceComparison priceComparison) {
        this.priceComparison = priceComparison;
    }
}