package com.example.price_comparator.controller;

import com.example.price_comparator.dto.ComparisonResponse;
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
import java.util.concurrent.CompletableFuture;

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

    @GetMapping("/trending")
    public ResponseEntity<List<ProductDocument>> getTrendingProducts() {
        logger.info("Received request for trending products");
        List<ProductDocument> products = productService.getTrendingProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<ProductDocument>> getProductById(@PathVariable String id) {
        logger.info("Received request for product with ID: {}", id);
        return productService.getProductById(id)
            .thenCompose(product -> {
                if (product != null) {
                    return CompletableFuture.completedFuture(ResponseEntity.ok(product));
                }
                return productService.fetchAndSaveProduct(id)
                    .thenApply(fetchedProduct -> {
                        if (fetchedProduct != null) {
                            return ResponseEntity.ok(fetchedProduct);
                        }
                        return ResponseEntity.notFound().build();
                    });
            });
    }

    @PostMapping("/{id}/enrich")
    public CompletableFuture<ResponseEntity<Void>> enrichProduct(@PathVariable String id) {
        return productService.getProductById(id).thenCompose(product -> {
            if (product != null) {
                return productService.enrichProduct(product).thenApply(p -> ResponseEntity.accepted().build());
            }
            return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
        });
    }

    @PostMapping("/{id}/compare")
    public CompletableFuture<ResponseEntity<String>> startShoppingComparison(@PathVariable String id) {
        return productService.getProductById(id)
            .thenCompose(product -> {
                if (product == null) {
                    return CompletableFuture.completedFuture(ResponseEntity.status(404).body("Product not found"));
                }
                return productService.enrichProduct(product)
                    .thenApply(enrichedProduct -> {
                        String taskId = productService.startShoppingComparison(enrichedProduct);
                        return ResponseEntity.ok(taskId);
                    });
            })
            .exceptionally(ex -> {
                logger.error("Error starting comparison for product {}", id, ex);
                return ResponseEntity.status(500).body("Error starting comparison");
            });
    }

    @GetMapping("/comparison/{taskId}")
    public ResponseEntity<ProductDocument> getComparisonResult(@PathVariable String taskId) {
        return productService.getComparisonResult(taskId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.accepted().build());
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

    @GetMapping
    public ResponseEntity<List<ProductDocument>> getAllProducts() {
        logger.info("Received request for all products");
        List<ProductDocument> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        logger.info("Received request for categories");
        List<String> categories = productService.getAmazonCategories();
        return ResponseEntity.ok(categories);
    }

    @PostMapping("/bookmarks/{userId}/{productId}")
    public ResponseEntity<Void> addBookmark(@PathVariable String userId, @PathVariable String productId) {
        productService.addBookmark(userId, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/bookmarks/{userId}/{productId}")
    public ResponseEntity<Void> removeBookmark(@PathVariable String userId, @PathVariable String productId) {
        productService.removeBookmark(userId, productId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/bookmarks/{userId}")
    public CompletableFuture<ResponseEntity<List<ProductDocument>>> getBookmarks(@PathVariable String userId) {
        return productService.getBookmarks(userId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(500).build());
    }
}
