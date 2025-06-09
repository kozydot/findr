package com.example.price_comparator;

import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.service.ScrapingService;

public class ScraperTestRunner {

    public static void main(String[] args) {
        ScrapingService scrapingService = new ScrapingService();

        String noonUrl = "https://www.noon.com/uae-en/iphone-15-128gb-black-5g-with-facetime-international-version/N53433298A/p/?o=e16a5e13b778631e&shareId=18146c95-8fe4-48a0-85c1-ce73de583841";
        String luluUrl = "https://gcc.luluhypermarket.com/en-ae/apple-iphone-16-pro-5g-smartphone-128-gb-storage-desert-titanium/p/2347787/";

        System.out.println("--- Testing Noon.com Scraper ---");
        System.out.println("URL: " + noonUrl);
        try {
            ProductDocument noonProduct = scrapingService.scrapeNoonProduct(noonUrl);
            if (noonProduct != null) {
                System.out.println("Scraped Product (Noon):");
                System.out.println(noonProduct.toString()); // Relies on Lombok @Data for toString()
            } else {
                System.out.println("Failed to scrape product from Noon.com or product not found.");
            }
        } catch (Exception e) {
            System.err.println("Error during Noon.com scraping test: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n--- Testing LuLu Hypermarket Scraper ---");
        System.out.println("URL: " + luluUrl);
        try {
            ProductDocument luluProduct = scrapingService.scrapeLuluProduct(luluUrl);
            if (luluProduct != null) {
                System.out.println("Scraped Product (LuLu):");
                System.out.println(luluProduct.toString()); // Relies on Lombok @Data for toString()
            } else {
                System.out.println("Failed to scrape product from LuLu Hypermarket or product not found.");
            }
        } catch (Exception e) {
            System.err.println("Error during LuLu Hypermarket scraping test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
