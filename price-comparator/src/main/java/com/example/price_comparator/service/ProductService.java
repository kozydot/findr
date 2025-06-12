package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final FirebaseService firebaseService;

    @Autowired
    public ProductService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    public List<ProductDocument> getFeaturedProducts(int limit) {
        logger.info("Fetching {} featured products", limit);
        // This would require a more complex query on Firebase, which is not straightforward.
        // For now, we'll return an empty list.
        return List.of();
    }

    public Optional<ProductDocument> getProductById(String id) {
        logger.info("Fetching product by ID: {}", id);
        return firebaseService.getProduct(id);
    }

    public List<ProductDocument> searchProducts(String query) {
        logger.info("Searching products with query: {}", query);
        // This would require a more complex query on Firebase, which is not straightforward.
        // For now, we'll return an empty list.
        return List.of();
    }

    public ProductDocument saveProduct(ProductDocument product) {
        logger.info("Saving product to Firebase: {}", product.getName());
        firebaseService.saveProduct(product);
        return product;
    }
}
