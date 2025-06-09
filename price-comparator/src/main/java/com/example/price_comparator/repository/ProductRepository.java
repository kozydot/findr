package com.example.price_comparator.repository;

import com.example.price_comparator.model.ProductDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<ProductDocument, String> {

    // Custom query methods can be added here if needed, for example:
    // List<ProductDocument> findByNameContainingIgnoreCase(String name);
    // List<ProductDocument> findByRetailersRetailerIdAndRetailersCurrentPriceLessThan(String retailerId, double price);
}
