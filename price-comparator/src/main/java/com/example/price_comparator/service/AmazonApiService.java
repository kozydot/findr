package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.model.RetailerInfo;
import com.example.price_comparator.model.SpecificationInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AmazonApiService implements RetailerApiService {

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(60))
            .build();

    private static final String API_KEY = "bd14942d97msh156a01a2a92cd6cp10be6cjsndfeb0c68787f";
    private static final String API_HOST = "real-time-amazon-data.p.rapidapi.com";

    public CompletableFuture<List<ProductDocument>> searchProducts(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://" + API_HOST + "/search?query=" + encodedQuery + "&page=1&country=AE&language=en_AE";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-rapidapi-key", API_KEY)
                .header("x-rapidapi-host", API_HOST)
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(responseBody -> {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.has("status") && !jsonResponse.getString("status").equals("OK")) {
                        System.err.println("API returned an error: " + jsonResponse.toString());
                        return new ArrayList<ProductDocument>();
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
                })
                .exceptionally(e -> {
                    System.err.println("Error searching Amazon products: " + e.getMessage());
                    return new ArrayList<>();
                });
    }

    public CompletableFuture<List<ProductDocument>> searchProductsByCategory(String category) {
        String encodedCategory = URLEncoder.encode(category, StandardCharsets.UTF_8);
        String url = "https://real-time-amazon-data.p.rapidapi.com/search?query=" + encodedCategory + "&page=1&country=AE&sort_by=RELEVANCE&product_condition=ALL&is_prime=false&deals_and_discounts=NONE&language=en_AE";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-rapidapi-key", API_KEY)
                .header("x-rapidapi-host", API_HOST)
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(responseBody -> {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.has("status") && !jsonResponse.getString("status").equals("OK")) {
                        System.err.println("API returned an error: " + jsonResponse.toString());
                        return new ArrayList<ProductDocument>();
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
                })
                .exceptionally(e -> {
                    System.err.println("Error searching Amazon products by category: " + e.getMessage());
                    return new ArrayList<>();
                });
    }

    @Cacheable("amazon-product-details")
    public CompletableFuture<ProductDocument> getProductDetails(String asin) {
        String url = "https://" + API_HOST + "/product-details?asin=" + asin + "&country=AE";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-rapidapi-key", API_KEY)
                .header("x-rapidapi-host", API_HOST)
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(responseBody -> {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.has("status") && !jsonResponse.getString("status").equals("OK")) {
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

                        // Extract other attributes
                        String model = extractAttribute(specifications, "model");
                        if (model == null) {
                            model = extractModelFromTitle(product.getName());
                        }
                        product.setModel(model);

                        product.setStorage(extractAttribute(specifications, "storage capacity"));
                        product.setRam(extractAttribute(specifications, "ram"));
                        product.setColor(extractAttribute(specifications, "color"));
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
                            product.getRetailers().add(amazonRetailer);
                            
                            product.setPrice(priceString);
                            product.setOriginalPrice(productJson.optString("product_original_price", null));
                            product.setCurrency(productJson.getString("currency"));

                        } catch (NumberFormatException e) {
                            System.err.println("Could not parse price for product " + product.getId() + ": " + priceString);
                        }
                    }

                    return product;
                })
                .exceptionally(e -> {
                    System.err.println("Error getting Amazon product details: " + e.getMessage());
                    return null;
                });
    }

    private String extractAttribute(List<SpecificationInfo> specifications, String attributeName) {
        return specifications.stream()
                .filter(s -> attributeName.equalsIgnoreCase(s.getName()))
                .map(SpecificationInfo::getValue)
                .findFirst()
                .orElse(null);
    }

    private String extractModelFromTitle(String title) {
        String lowerCaseTitle = title.toLowerCase();
        if (lowerCaseTitle.contains("iphone")) {
            return extractToken(title, "iPhone");
        }
        if (lowerCaseTitle.contains("galaxy")) {
            return extractToken(title, "Galaxy");
        }
        if (lowerCaseTitle.contains("macbook")) {
            return extractToken(title, "MacBook");
        }
        if (lowerCaseTitle.contains("dell")) {
            return extractToken(title, "Dell");
        }
        if (lowerCaseTitle.contains("hp")) {
            return extractToken(title, "HP");
        }
        if (lowerCaseTitle.contains("lenovo")) {
            return extractToken(title, "Lenovo");
        }
        return null;
    }

    private String extractToken(String title, String brand) {
        String[] parts = title.split(brand);
        if (parts.length > 1) {
            String[] modelParts = parts[1].trim().split(" ");
            if (modelParts.length > 0) {
                return modelParts[0];
            }
        }
        return null;
    }

}
