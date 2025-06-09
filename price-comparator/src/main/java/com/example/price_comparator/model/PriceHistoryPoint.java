package com.example.price_comparator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// Consider using java.time.Instant or java.time.LocalDate for the date
// if more specific date/time handling is needed.
// For simplicity with MongoDB and potential frontend string dates, String is used here.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistoryPoint {

    private String date; 
    private double price;
}
