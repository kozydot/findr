package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final FirebaseService firebaseService;
    // private final ScrapingService scrapingService; // To be used later

    @Autowired
    public ProductService(ProductRepository productRepository, FirebaseService firebaseService /*, ScrapingService scrapingService */) {
        this.productRepository = productRepository;
        this.firebaseService = firebaseService;
        // this.scrapingService = scrapingService;
    }

    public List<ProductDocument> getFeaturedProducts(int limit) {
        logger.info("Fetching {} featured products", limit);
        // Basic implementation: returns the first 'limit' products.
        // A more sophisticated version might look for products with a 'featured' flag or high rating.
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findAll(pageable).getContent();
    }

    public Optional<ProductDocument> getProductById(String id) {
        logger.info("Fetching product by ID: {}", id);
        // Example: Potentially trigger a scrape if product is not found or data is stale
        // Optional<ProductDocument> product = productRepository.findById(id);
        // if (product.isEmpty() || isDataStale(product.get())) {
        //     ProductDocument scrapedProduct = scrapingService.scrapeProductFromSomeSource(id_or_url);
        //     if (scrapedProduct != null) {
        //         return Optional.of(productRepository.save(scrapedProduct));
        //     }
        // }
        return productRepository.findById(id);
    }

    public List<ProductDocument> searchProducts(String query) {
        logger.info("Searching products with query: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return productRepository.findAll(PageRequest.of(0, 20)).getContent(); // Return some default if query is empty
        }
        // Basic case-insensitive search on product name and description.
        // MongoDB supports regex queries, which can be powerful.
        // This is a simplified example. For more complex searching, consider dedicated search fields or text indexes.
        // Using a simple regex for demonstration. For production, ensure proper escaping and consider performance.
        String escapedQuery = Pattern.quote(query);
        // This is a conceptual example; actual implementation with MongoRepository would use @Query or Criteria API.
        // For now, let's assume a custom method in ProductRepository or filter in service.
        // This will be refined when ProductRepository custom methods are implemented.
        // For simplicity, returning all products for now if a direct repository method isn't defined yet.
        // A more robust solution would be:
        // return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
        
        // Temporary: filter in memory (not efficient for large datasets)
        // This should be replaced by a proper database query.
        List<ProductDocument> allProducts = productRepository.findAll();
        return allProducts.stream()
                .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(query.toLowerCase())) ||
                             (p.getDescription() != null && p.getDescription().toLowerCase().contains(query.toLowerCase())))
                .toList();
    }

    // Placeholder for data staleness check
    // private boolean isDataStale(ProductDocument product) {
    //     // Logic to determine if product data needs refreshing
    //     // e.g., based on a lastUpdated timestamp
    //     return false; 
    // }

    // Method to save or update a product (e.g., after scraping)
    public ProductDocument saveProduct(ProductDocument product) {
        logger.info("Saving product to MongoDB: {}", product.getName());
        ProductDocument savedProduct = productRepository.save(product);
        logger.info("Saving product to Firebase: {}", product.getName());
        firebaseService.saveProduct(savedProduct);
        return savedProduct;
    }
}
