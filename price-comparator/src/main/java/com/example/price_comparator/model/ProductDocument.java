package com.example.price_comparator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ProductDocument {

    private String id;
    private String name;
    private String brand;
    private String description;
    private String model;
    private String storage;
    private String ram;    private String color;
    private String imageUrl;
    private String imagePerceptualHash;  // Perceptual hash for visual similarity
    private String imageMD5Hash;         // MD5 hash for exact image matching
    private double rating;
    private int reviews;
    @JsonProperty("price")
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

    // Default constructor
    public ProductDocument() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getRam() {
        return ram;
    }

    public void setRam(String ram) {
        this.ram = ram;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getImageUrl() {
        return imageUrl;
    }    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImagePerceptualHash() {
        return imagePerceptualHash;
    }

    public void setImagePerceptualHash(String imagePerceptualHash) {
        this.imagePerceptualHash = imagePerceptualHash;
    }

    public String getImageMD5Hash() {
        return imageMD5Hash;
    }

    public void setImageMD5Hash(String imageMD5Hash) {
        this.imageMD5Hash = imageMD5Hash;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviews() {
        return reviews;
    }

    public void setReviews(int reviews) {
        this.reviews = reviews;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(String originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public List<String> getAbout() {
        return about;
    }

    public void setAbout(List<String> about) {
        this.about = about;
    }

    public Map<String, String> getProductInformation() {
        return productInformation;
    }

    public void setProductInformation(Map<String, String> productInformation) {
        this.productInformation = productInformation;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public List<RetailerInfo> getRetailers() {
        return retailers;
    }

    public void setRetailers(List<RetailerInfo> retailers) {
        this.retailers = retailers;
    }

    public List<SpecificationInfo> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(List<SpecificationInfo> specifications) {
        this.specifications = specifications;
    }

    public Date getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(Date lastChecked) {
        this.lastChecked = lastChecked;
    }

}
