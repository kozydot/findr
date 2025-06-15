package com.example.price_comparator.utils;

import org.json.JSONObject;

public class JsonUtils {
    public static String optString(JSONObject json, String key, String fallback) {
        return json.has(key) && !json.isNull(key) ? json.getString(key) : fallback;
    }
}