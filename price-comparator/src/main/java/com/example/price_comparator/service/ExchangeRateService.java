package com.example.price_comparator.service;

import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {

    // In a real application, this would call an external API.
    // For this example, we'll use a static rate.
    private static final double USD_TO_AED_RATE = 3.67;

    public double getUsdToAedRate() {
        return USD_TO_AED_RATE;
    }
}