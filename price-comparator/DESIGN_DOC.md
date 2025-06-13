# Findr: Amazon-to-AliExpress Price Comparison Algorithm

## 1. Overview

This document outlines the design for a multi-factor matching algorithm to find the most accurate equivalent of an Amazon.ae product on AliExpress. The goal is to provide a robust and reliable price comparison for users of the "Findr" application.

## 2. Architecture

The core of this feature will be a new, dedicated service, `ProductMatchingService`. This service will encapsulate the entire matching and comparison logic, promoting a clean separation of concerns. The existing `ProductService` will delegate the comparison task to this new service.

### 2.1. Data Flow Diagram

```mermaid
sequenceDiagram
    participant C as Client
    participant PC as ProductController
    participant PS as ProductService
    participant AAS as AmazonApiService
    participant PMS as ProductMatchingService
    participant AliS as AliexpressApiService
    participant ExchangeRate as ExchangeRateService
    participant ImageHasher as ImageHashingService

    C->>PC: GET /api/v1/products/compare/{amazon_product_id}
    PC->>PS: findComparison(amazon_product_id)
    PS->>AAS: getProductDetails(amazon_product_id)
    AAS-->>PS: return amazonProductData
    PS->>PMS: findBestMatch(amazonProductData)
    
    subgraph ProductMatchingService Logic
        PMS->>PMS: 1. generateSearchQuery(amazonProductData)
        PMS->>AliS: 2. searchProducts(query, shipTo='AE', currency='USD')
        AliS-->>PMS: return aliexpress_candidates[]
        PMS->>PMS: 3. initialFilter(candidates)
        
        loop For each candidate
            PMS->>PMS: 4a. calculateTitleScore(amazon.title, candidate.title)
            PMS->>ImageHasher: 4b. calculateImageScore(amazon.imageUrl, candidate.imageUrl)
            PMS->>PMS: 4c. calculateSellerScore(candidate.saleVolume)
            PMS->>PMS: 4d. computeWeightedMatchScore(...)
        end
        
        PMS->>PMS: 5. selectBestMatch(scored_candidates)
    end
    
    alt Match Found (score > 0.7)
        PMS->>ExchangeRate: convertUSDToAED(aliexpress_price_usd)
        ExchangeRate-->>PMS: return price_aed
        PMS-->>PS: return ComparisonResult (with match)
    else No Confident Match
        PMS-->>PS: return ComparisonResult (no match)
    end
    
    PS-->>PC: return result
    PC-->>C: return JSON Response
```

## 3. Detailed Algorithm Pseudocode

The core logic will be implemented in a `find_and_compare` function within the `ProductMatchingService`.

```pseudocode
FUNCTION find_and_compare(amazon_product):
    // --- Constants ---
    DEFINE WEIGHT_TITLE = 0.45
    DEFINE WEIGHT_IMAGE = 0.40
    DEFINE WEIGHT_SELLER = 0.15
    DEFINE MIN_CONFIDENCE_THRESHOLD = 0.70

    // --- Step 1: Smart Search Query Generation ---
    search_term = amazon_product.brand + " " + amazon_product.model_number
    IF amazon_product.model_number is empty or null:
        // Fallback: use the first few significant words from the title
        search_term = extract_significant_keywords(amazon_product.title)
    
    sanitized_query = sanitize_and_url_encode(search_term)

    // --- Step 2: API Call and Initial Filtering ---
    aliexpress_results = call_aliexpress_api(
        searchTerm=sanitized_query,
        shipToCountry="AE",
        currency="USD"
    )

    IF aliexpress_results is empty or failed:
        RETURN { "match_found": false, "message": "API call to AliExpress failed or returned no results." }

    // Preliminary filtering
    filtered_candidates = []
    amazon_brand_lower = lowercase(amazon_product.brand)
    FOR product IN aliexpress_results.products:
        IF lowercase(product.productTitle) contains amazon_brand_lower:
            add product to filtered_candidates

    IF filtered_candidates is empty:
        RETURN { "match_found": false, "message": "No products found on AliExpress matching the brand." }

    // --- Step 3: Multi-Factor Match Scoring ---
    scored_candidates = []
    max_sale_volume = find_max_value([p.latestSaleVolume for p in filtered_candidates])

    FOR candidate IN filtered_candidates:
        // 3a. Title Similarity Score
        title_score = calculate_jaccard_similarity(
            normalize_text(amazon_product.title),
            normalize_text(candidate.productTitle)
        )
        // Bonus for model number presence
        IF candidate.productTitle contains amazon_product.model_number:
            title_score = title_score * 1.20 // 20% bonus
            title_score = min(1.0, title_score) // Cap at 1.0

        // 3b. Image Similarity Score
        amazon_hash = calculate_image_phash(amazon_product.main_image_url)
        candidate_hash = calculate_image_phash(candidate.mainImageUrl)
        hamming_distance = calculate_hamming_distance(amazon_hash, candidate_hash)
        // Score is inversely proportional to distance. Max distance of 64 for pHash.
        // A distance of 0-5 is a likely match. >10 is different.
        image_score = (64 - hamming_distance) / 64

        // 3c. Seller/Listing Quality Score
        // Normalize based on the max sales volume in this specific result set
        seller_score = 0.0
        IF max_sale_volume > 0:
            seller_score = candidate.latestSaleVolume / max_sale_volume

        // 3d. Composite Match Score
        composite_score = (title_score * WEIGHT_TITLE) + 
                          (image_score * WEIGHT_IMAGE) + 
                          (seller_score * WEIGHT_SELLER)
        
        add { "product": candidate, "score": composite_score } to scored_candidates

    // --- Step 4: Best Match Selection ---
    best_match = null
    highest_score = 0.0
    FOR scored_candidate IN scored_candidates:
        IF scored_candidate.score > highest_score:
            highest_score = scored_candidate.score
            best_match = scored_candidate.product

    IF best_match is null OR highest_score < MIN_CONFIDENCE_THRESHOLD:
        RETURN {
          "match_found": false,
          "message": "No confident match found on AliExpress for the given product."
        }

    // --- Step 5: Final Price Comparison ---
    aliexpress_price_usd = best_match.salePrice
    exchange_rate_usd_to_aed = get_current_exchange_rate("USD", "AED")
    aliexpress_price_aed = aliexpress_price_usd * exchange_rate_usd_to_aed

    price_difference = amazon_product.price_aed - aliexpress_price_aed
    cheaper_store = "Amazon" IF price_difference < 0 ELSE "AliExpress"

    RETURN {
      "match_found": true,
      "amazon_product": { "title": amazon_product.title, "price_aed": amazon_product.price_aed },
      "aliexpress_match": {
        "productTitle": best_match.productTitle,
        "productDetailUrl": best_match.productDetailUrl,
        "salePriceUSD": best_match.salePrice,
        "shopName": best_match.shopName
      },
      "match_score": highest_score,
      "price_comparison": {
        "amazon_price_aed": round(aliexpress_price_aed, 2),
        "aliexpress_price_aed": round(aliexpress_price_aed, 2),
        "price_difference_aed": round(abs(price_difference), 2),
        "cheaper_store": cheaper_store,
        "notes": "Shipping costs not included. Verify final price on seller's page."
      }
    }

// --- Helper Functions (to be implemented) ---
FUNCTION normalize_text(text):
    // Lowercase, remove punctuation, remove common stop words
    ...
FUNCTION calculate_jaccard_similarity(set1, set2):
    // |set1 intersect set2| / |set1 union set2|
    ...
FUNCTION calculate_image_phash(image_url):
    // Download image, compute perceptual hash
    ...
FUNCTION calculate_hamming_distance(hash1, hash2):
    // Count differing bits between two hashes
    ...
FUNCTION get_current_exchange_rate(from_currency, to_currency):
    // Call an external or internal exchange rate service
    ...