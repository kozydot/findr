package com.example.price_comparator.dto;

import com.example.price_comparator.model.SpecificationInfo;
import lombok.Data;
import java.util.List;

@Data
public class ShoppingProduct {
    private String title;
    private double price;
    private String seller;    private String productLink;    private String imageUrl;
    private String description;
    private List<SpecificationInfo> specifications;
}