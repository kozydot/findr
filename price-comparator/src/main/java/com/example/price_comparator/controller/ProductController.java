package com.example.price_comparator.controller;

import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.service.ProductService;
// Import ScrapingService if you want to add a manual trigger endpoint
// import com.example.price_comparator.service.ScrapingService; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/products")
// @CrossOrigin(origins = "http://localhost:5173") // Or your frontend's actual origin in development
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    // private final ScrapingService scrapingService; // For manual scrape trigger

    @Autowired
    public ProductController(ProductService productService /*, ScrapingService scrapingService */) {
        this.productService = productService;
        // this.scrapingService = scrapingService;
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ProductDocument>> getFeaturedProducts(@RequestParam(defaultValue = "6") int limit) {
        logger.info("Received request for {} featured products", limit);
        List<ProductDocument> products = productService.getFeaturedProducts(Math.max(1, Math.min(limit, 20))); // Cap limit
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDocument> getProductById(@PathVariable String id) {
        logger.info("Received request for product with ID: {}", id);
        Optional<ProductDocument> product = productService.getProductById(id);
        return product.map(ResponseEntity::ok)
                      .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDocument>> searchProducts(@RequestParam String q) {
        logger.info("Received search request with query: {}", q);
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().build(); // Or return empty list
        }
        List<ProductDocument> products = productService.searchProducts(q);
        return ResponseEntity.ok(products);
    }

    // Example endpoint to manually trigger a scrape for a specific Noon URL (for testing)
    // This is optional and might be removed or secured in a production environment.
    /*
    @PostMapping("/scrape/noon")
    public ResponseEntity<?> triggerNoonScrape(@RequestBody String productUrl) {
        if (productUrl == null || productUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Product URL is required.");
        }
        logger.info("Received manual scrape request for Noon URL: {}", productUrl);
        ProductDocument scrapedProduct = scrapingService.scrapeProductFromNoon(productUrl);
        if (scrapedProduct != null) {
            productService.saveProduct(scrapedProduct);
            return ResponseEntity.ok(scrapedProduct);
        } else {
            return ResponseEntity.status(500).body("Failed to scrape product from Noon.");
        }
    }
    */

    // TODO: Add POST/PUT/DELETE endpoints if direct product management via API is needed.
    // TODO: Implement proper error handling and response statuses.
    // TODO: Configure CORS properly. The @CrossOrigin annotation is commented out;
    //       a global CORS configuration is generally preferred (e.g., in a WebMvcConfigurer bean).
}
