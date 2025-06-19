package com.example.price_comparator.utils;

import org.json.JSONObject;

public class JsonUtils {
      /**
     * Enhanced optString that handles different data types safely
     * Converts numbers, booleans, and other types to strings when needed
     */
    public static String optString(JSONObject json, String key, String fallback) {
        if (json == null || !json.has(key) || json.isNull(key)) {
            return fallback;
        }
        
        try {
            // First try to get as object to avoid type casting issues
            Object value = json.opt(key);
            if (value == null) {
                return fallback;
            }
            
            if (value instanceof String) {
                return (String) value;
            } else if (value instanceof Number) {
                // Handle numeric values like ratings, prices, etc.
                // For ratings, format with one decimal place
                if (key.equals("rating") && value instanceof Number) {
                    return String.format("%.1f", ((Number) value).doubleValue());
                }
                return value.toString();
            } else if (value instanceof Boolean) {
                // Handle boolean values
                return value.toString();
            } else {
                // For any other type, convert to string
                return value.toString();
            }
        } catch (Exception e) {
            // If any conversion fails, return fallback
            return fallback;
        }
    }
    
    /**
     * Get a numeric value as double, with fallback
     */
    public static double optDouble(JSONObject json, String key, double fallback) {
        if (!json.has(key) || json.isNull(key)) {
            return fallback;
        }
        
        try {
            return json.getDouble(key);
        } catch (Exception e) {
            return fallback;
        }
    }
    
    /**
     * Get an integer value, with fallback
     */
    public static int optInt(JSONObject json, String key, int fallback) {
        if (!json.has(key) || json.isNull(key)) {
            return fallback;
        }
        
        try {
            return json.getInt(key);
        } catch (Exception e) {
            return fallback;
        }
    }
}