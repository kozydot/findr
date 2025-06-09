package com.example.price_comparator.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class ProductDocument {

    @Id
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private double rating;
    private int reviews;
    private List<RetailerInfo> retailers;
    private List<SpecificationInfo> specifications;
}
