package com.example.price_comparator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {

    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private double rating;
    private int reviews;
    private String price;
    private String originalPrice;
    private String currency;
    private String productUrl;
    private String availability;
    private List<String> about;
    private Map<String, String> productInformation;
    private List<String> photos;
    private List<RetailerInfo> retailers = new ArrayList<>();
    private List<SpecificationInfo> specifications;
    private Date lastChecked;
}
