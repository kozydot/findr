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
    private final PriceApiService priceApiService;

    @Autowired
    public ProductService(FirebaseService firebaseService, PriceApiService priceApiService) {
        this.firebaseService = firebaseService;
        this.priceApiService = priceApiService;
    }

    public List<ProductDocument> getFeaturedProducts(int limit) {
        logger.info("Fetching {} featured products", limit);
        List<ProductDocument> allProducts = firebaseService.getAllProducts();
        return allProducts.subList(0, Math.min(limit, allProducts.size()));
    }

    public List<ProductDocument> getTrendingProducts() {
        logger.info("Fetching trending products");
        return priceApiService.fetchProductData("amazon", "electronics");
    }

    public Optional<ProductDocument> getProductById(String id) {
        logger.info("Fetching product by ID: {}", id);
        return firebaseService.getProduct(id);
    }

    public List<ProductDocument> getAllProducts() {
        logger.info("Fetching all products");
        return firebaseService.getAllProducts();
    }

    public List<ProductDocument> searchProducts(String query) {
        logger.info("Searching products with query: {}", query);
        // This would require a more complex query on Firebase, which is not straightforward.
        // For now, we'll return an empty list.
        return List.of();
    }

    public List<String> getAmazonCategories() {
        logger.info("Fetching categories from Amazon");
        // This is a placeholder. In a real application, you would have a mechanism
        // to fetch and store categories from Amazon.
        return List.of("Electronics", "Computers", "Smart Home", "Video Games");
    }

    public ProductDocument saveProduct(ProductDocument product) {
        logger.info("Saving product to Firebase: {}", product.getName());
        firebaseService.saveProduct(product);
        return product;
    }

    public ProductDocument compareAndSaveProduct(String productId) {
        Optional<ProductDocument> productOptional = getProductById(productId);
        if (productOptional.isEmpty()) {
            return null;
        }

        ProductDocument product = productOptional.get();
        
        ProductDocument amazonProduct = priceApiService.getProductDetails("amazon", productId);
        if (amazonProduct != null) {
            product.getRetailers().addAll(amazonProduct.getRetailers());
        }

        ProductDocument aliexpressProduct = priceApiService.getProductDetails("aliexpress", productId);
        if (aliexpressProduct != null) {
            product.getRetailers().addAll(aliexpressProduct.getRetailers());
        }

        return saveProduct(product);
    }

    public void updateAllProducts(String query) {
        logger.info("Fetching products from Amazon for query: {}", query);
        List<ProductDocument> amazonProducts = priceApiService.fetchProductData("amazon", query);
        if (amazonProducts != null && !amazonProducts.isEmpty()) {
            for (ProductDocument product : amazonProducts) {
                if (product != null && product.getName() != null) {
                    saveProduct(product);
                    logger.info("Successfully processed product data for: " + product.getName());
                }
            }
        } else {
            logger.error("Failed to fetch product data from Amazon for query: " + query);
        }

        logger.info("Fetching products from Aliexpress for query: {}", query);
        List<ProductDocument> aliexpressProducts = priceApiService.fetchProductData("aliexpress", query);
        if (aliexpressProducts != null && !aliexpressProducts.isEmpty()) {
            for (ProductDocument product : aliexpressProducts) {
                if (product != null && product.getName() != null) {
                    saveProduct(product);
                    logger.info("Successfully processed product data for: " + product.getName());
                }
            }
        } else {
            logger.error("Failed to fetch product data from Aliexpress for query: " + query);
        }
    }
}
