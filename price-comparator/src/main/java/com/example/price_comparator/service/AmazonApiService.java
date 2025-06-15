package com.example.price_comparator.service;

import com.example.price_comparator.model.PriceHistoryPoint;
import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.model.RetailerInfo;
import com.example.price_comparator.model.SpecificationInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AmazonApiService implements RetailerApiService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private static final String API_KEY = "630ee18098msh8b902cd1f9e530cp1203a7jsn3f3a585e3b1c";
    private static final String API_HOST = "real-time-amazon-data.p.rapidapi.com";

    public List<ProductDocument> searchProducts(String query) {
        try {
            String url = "https://" + API_HOST + "/search?query=" + query + "&page=1&country=AE&language=en_AE";
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("x-rapidapi-key", API_KEY)
                    .addHeader("x-rapidapi-host", API_HOST)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                System.err.println("API request failed with code: " + response.code());
                return new ArrayList<>();
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);

            if (!jsonResponse.getString("status").equals("OK")) {
                System.err.println("API returned an error: " + jsonResponse.toString());
                return new ArrayList<>();
            }

            List<ProductDocument> products = new ArrayList<>();
            JSONArray productsJson = jsonResponse.getJSONObject("data").getJSONArray("products");
            for (int i = 0; i < productsJson.length(); i++) {
                JSONObject productJson = productsJson.getJSONObject(i);
                ProductDocument product = new ProductDocument();
                product.setId(productJson.optString("asin", null));
                product.setName(productJson.optString("product_title", null));
                product.setProductUrl(productJson.optString("product_url", null));
                product.setImageUrl(productJson.optString("product_photo", null));
                product.setRating(productJson.optDouble("product_star_rating", 0.0));
                product.setReviews(productJson.optInt("product_num_ratings", 0));
                product.setCurrency(jsonResponse.getJSONObject("parameters").optString("country", null));

                String priceString = productJson.optString("product_price", null);
                if (priceString != null && !priceString.isEmpty()) {
                    try {
                        RetailerInfo amazonRetailer = new RetailerInfo();
                        amazonRetailer.setRetailerId("amazon");
                        amazonRetailer.setName("Amazon.ae");
                        amazonRetailer.setLogo("https://upload.wikimedia.org/wikipedia/commons/a/a9/Amazon_logo.svg");
                        amazonRetailer.setProductUrl(product.getProductUrl());
                        
                        String priceDigits = priceString.replaceAll("[^\\d.]", "");
                        amazonRetailer.setCurrentPrice(Double.parseDouble(priceDigits));
                        
                        amazonRetailer.setInStock(true); 
                        
                        product.getRetailers().add(amazonRetailer);
                        product.setPrice(priceString);

                    } catch (NumberFormatException e) {
                        System.err.println("Could not parse price for product " + product.getId() + ": " + priceString);
                    }
                }
                products.add(product);
            }
            return products;

        } catch (IOException e) {
            System.err.println("Error searching Amazon products: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<ProductDocument> searchProductsByCategory(String category) {
        try {
            String url = "https://real-time-amazon-data.p.rapidapi.com/search?query=" + category + "&page=1&country=AE&sort_by=RELEVANCE&product_condition=ALL&is_prime=false&deals_and_discounts=NONE&language=en_AE";
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("x-rapidapi-key", API_KEY)
                    .addHeader("x-rapidapi-host", API_HOST)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                System.err.println("API request failed with code: " + response.code());
                return new ArrayList<>();
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);

            if (!jsonResponse.getString("status").equals("OK")) {
                System.err.println("API returned an error: " + jsonResponse.toString());
                return new ArrayList<>();
            }

            List<ProductDocument> products = new ArrayList<>();
            JSONArray productsJson = jsonResponse.getJSONObject("data").getJSONArray("products");
            for (int i = 0; i < productsJson.length(); i++) {
                JSONObject productJson = productsJson.getJSONObject(i);
                ProductDocument product = new ProductDocument();
                product.setId(productJson.optString("asin", null));
                product.setName(productJson.optString("product_title", null));
                product.setProductUrl(productJson.optString("product_url", null));
                product.setImageUrl(productJson.optString("product_photo", null));
                product.setRating(productJson.optDouble("product_star_rating", 0.0));
                product.setReviews(productJson.optInt("product_num_ratings", 0));
                product.setCurrency(jsonResponse.getJSONObject("parameters").optString("country", null));

                String priceString = productJson.optString("product_price", null);
                if (priceString != null && !priceString.isEmpty()) {
                    try {
                        RetailerInfo amazonRetailer = new RetailerInfo();
                        amazonRetailer.setRetailerId("amazon");
                        amazonRetailer.setName("Amazon.ae");
                        amazonRetailer.setLogo("https://upload.wikimedia.org/wikipedia/commons/a/a9/Amazon_logo.svg");
                        amazonRetailer.setProductUrl(product.getProductUrl());
                        
                        String priceDigits = priceString.replaceAll("[^\\d.]", "");
                        amazonRetailer.setCurrentPrice(Double.parseDouble(priceDigits));
                        
                        amazonRetailer.setInStock(true);
                        
                        product.getRetailers().add(amazonRetailer);
                        product.setPrice(priceString);

                    } catch (NumberFormatException e) {
                        System.err.println("Could not parse price for product " + product.getId() + ": " + priceString);
                    }
                }
                products.add(product);
            }
            return products;

        } catch (IOException e) {
            System.err.println("Error searching Amazon products by category: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Cacheable("amazon-product-details")
    public ProductDocument getProductDetails(String asin) {
        try {
            String url = "https://" + API_HOST + "/product-details?asin=" + asin + "&country=AE";
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("x-rapidapi-key", API_KEY)
                    .addHeader("x-rapidapi-host", API_HOST)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                System.err.println("API request failed with code: " + response.code());
                return null;
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);

            if (!jsonResponse.getString("status").equals("OK")) {
                System.err.println("API returned an error: " + jsonResponse.toString());
                return null;
            }

            JSONObject productJson = jsonResponse.getJSONObject("data");
            ProductDocument product = new ProductDocument();
            product.setId(productJson.optString("asin", null));
            product.setName(productJson.getString("product_title"));
            product.setProductUrl(productJson.getString("product_url"));
            product.setImageUrl(productJson.getString("product_photo"));
            product.setDescription(productJson.optString("product_description", null));
            product.setRating(productJson.optDouble("product_star_rating", 0.0));
            product.setReviews(productJson.getInt("product_num_ratings"));
            product.setAbout(productJson.getJSONArray("about_product").toList().stream().map(Object::toString).collect(Collectors.toList()));
            
            JSONObject productInfoJson = productJson.optJSONObject("product_information");
            if (productInfoJson != null) {
                List<SpecificationInfo> specifications = new ArrayList<>();
                for (String key : productInfoJson.keySet()) {
                    Object value = productInfoJson.get(key);
                    specifications.add(new SpecificationInfo(key, value.toString()));
                }
                product.setSpecifications(specifications);

                // Attempt to extract brand from specifications
                String brand = specifications.stream()
                    .filter(s -> "brand".equalsIgnoreCase(s.getName()))
                    .map(SpecificationInfo::getValue)
                    .findFirst()
                    .orElse(null);

                if (brand == null) {
                    // Fallback: attempt to extract brand from the product title
                    String title = product.getName().toLowerCase();
                    if (title.contains("samsung")) brand = "Samsung";
                    else if (title.contains("apple")) brand = "Apple";
                    else if (title.contains("sony")) brand = "Sony";
                    else if (title.contains("lg")) brand = "LG";
                    else if (title.contains("dell")) brand = "Dell";
                    else if (title.contains("hp")) brand = "HP";
                    else if (title.contains("lenovo")) brand = "Lenovo";
                    else if (title.contains("redragon")) brand = "Redragon";
                }
                product.setBrand(brand);
            }

            product.setPhotos(productJson.getJSONArray("product_photos").toList().stream().map(Object::toString).collect(Collectors.toList()));

            String priceString = productJson.optString("product_price", null);
            if (priceString != null && !priceString.isEmpty()) {
                try {
                    RetailerInfo amazonRetailer = new RetailerInfo();
                    amazonRetailer.setRetailerId("amazon");
                    amazonRetailer.setName("Amazon.ae");
                    amazonRetailer.setLogo("https://upload.wikimedia.org/wikipedia/commons/a/a9/Amazon_logo.svg");
                    amazonRetailer.setProductUrl(product.getProductUrl());

                    String priceDigits = priceString.replaceAll("[^\\d.]", "");
                    amazonRetailer.setCurrentPrice(Double.parseDouble(priceDigits));

                    String availability = productJson.optString("product_availability", "");
                    amazonRetailer.setInStock(availability != null && availability.toLowerCase().contains("in stock"));

                    String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                    amazonRetailer.getPriceHistory().add(new PriceHistoryPoint(currentDate, amazonRetailer.getCurrentPrice()));
                    product.getRetailers().add(amazonRetailer);
                    
                    product.setPrice(priceString);
                    product.setOriginalPrice(productJson.optString("product_original_price", null));
                    product.setCurrency(productJson.getString("currency"));

                } catch (NumberFormatException e) {
                    System.err.println("Could not parse price for product " + product.getId() + ": " + priceString);
                }
            }

            return product;

        } catch (IOException e) {
            System.err.println("Error getting Amazon product details: " + e.getMessage());
            return null;
        }
    }
}
