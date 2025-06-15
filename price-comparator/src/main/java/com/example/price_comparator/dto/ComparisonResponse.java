package com.example.price_comparator.dto;

import com.example.price_comparator.model.ProductDocument;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class ComparisonResponse {
    private String taskId;
    private ProductDocument product;
}