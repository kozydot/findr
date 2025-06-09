package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.model.RetailerInfo;
import com.example.price_comparator.model.SpecificationInfo;
import com.example.price_comparator.model.PriceHistoryPoint; // Assuming this exists for RetailerInfo

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScrapingService {

    private static final String NOON_RETAILER_ID = "noon";
    private static final String NOON_RETAILER_NAME = "Noon.com";

    public ProductDocument scrapeNoonProduct(String productUrl) {
        try {
            Document doc = Jsoup.connect(productUrl)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                .header("Accept-Language", "en-US,en;q=0.9")
                                .header("Accept-Encoding", "gzip, deflate, br")
                                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                                .header("Connection", "keep-alive")
                                .ignoreContentType(true) // Add this
                                .timeout(60000) // Increased to 60 seconds timeout
                                .get();

            ProductDocument product = new ProductDocument();

            // Product Name
            Element nameElement = doc.selectFirst("h1.CoreDetails_productTitle__JCoTk");
            if (nameElement != null) {
                product.setName(nameElement.text());
            }

            // Main Image URL
            Element imageElement = doc.selectFirst("div.Gallery_sliderInnerCtr__eEbFL > div:first-child .ProductImage_imageWrapper__C_aHA img");
            if (imageElement != null) {
                product.setImageUrl(imageElement.attr("src"));
            }

            // Overview/Description
            Element overviewElement = doc.selectFirst("div.OverviewTab_overviewDescriptionCtr__d5ELj");
            if (overviewElement != null) {
                Element labelSpan = overviewElement.selectFirst("span.OverviewDescription_label__2mWfB");
                String overviewText = overviewElement.text();
                if (labelSpan != null) {
                    overviewText = overviewText.replace(labelSpan.text(), "").trim();
                }
                product.setDescription(overviewText);
            }

            // Retailer Specific Info
            RetailerInfo noonRetailerInfo = new RetailerInfo();
            noonRetailerInfo.setRetailerId(NOON_RETAILER_ID);
            noonRetailerInfo.setName(NOON_RETAILER_NAME);
            noonRetailerInfo.setProductUrl(productUrl);
            noonRetailerInfo.setInStock(true); // Default assumption
            noonRetailerInfo.setPriceHistory(new ArrayList<>()); // Initialize empty

            // Current Price
            Element currentPriceValElement = doc.selectFirst("div[data-qa=div-price-now] span.PriceOffer_priceNowText__08sYH");
            if (currentPriceValElement != null) {
                String priceStr = currentPriceValElement.text().replaceAll("[^\\d.]", "");
                if (!priceStr.isEmpty()) {
                    noonRetailerInfo.setCurrentPrice(Double.parseDouble(priceStr));
                }
            }
            // Note: Currency symbol (e.g., "") is scraped but not stored in current RetailerInfo model

            List<RetailerInfo> retailers = new ArrayList<>();
            retailers.add(noonRetailerInfo);
            product.setRetailers(retailers);

            // Specifications (including Brand and Original Price as specs)
            Map<String, String> specsMap = new HashMap<>();

            // Brand
            Element brandElement = doc.selectFirst("div.CoreDetails_brand__WlUtB");
            if (brandElement != null) {
                specsMap.put("Brand", brandElement.text());
            }
            
            // Original Price (add as a specification)
            Element originalPriceContainer = doc.selectFirst("div.PriceOffer_priceWasCtr__qwKoN[data-qa=div-price-was]");
            if (originalPriceContainer != null) {
                Elements priceSpans = originalPriceContainer.select("span");
                 String originalPriceText = "";
                if (priceSpans.size() >= 2) { 
                    originalPriceText = priceSpans.last().text().replaceAll("[^\\d.]", "");
                } else if (priceSpans.size() == 1 && priceSpans.first().text().matches(".*\\d.*")){
                    originalPriceText = priceSpans.first().text().replaceAll("[^\\d.]", "");
                }
                if (!originalPriceText.isEmpty()) {
                    // Also try to get currency symbol for original price if available
                    String originalPriceCurrency = "";
                    if (priceSpans.size() >= 1 && !priceSpans.first().text().matches(".*\\d.*")) {
                         originalPriceCurrency = priceSpans.first().text().trim() + " ";
                    } else if (priceSpans.size() >= 2 && !priceSpans.get(0).text().matches(".*\\d.*")) {
                         originalPriceCurrency = priceSpans.get(0).text().trim() + " ";
                    }
                    specsMap.put("Original Price", originalPriceCurrency + originalPriceText);
                }
            }

            Element specsTableElement = doc.selectFirst("div.SpecificationsTab_tableCtr__L9BeQ table");
            if (specsTableElement != null) {
                Elements rows = specsTableElement.select("tr");
                for (Element row : rows) {
                    Elements specNameElements = row.select("td.SpecificationsTab_specName__AHFgu");
                    Elements specValueElements = row.select("td.SpecificationsTab_specValue__IMRv5");

                    if (!specNameElements.isEmpty() && !specValueElements.isEmpty()) {
                        String specName = specNameElements.first().text();
                        String specValue = specValueElements.first().text();
                        specsMap.put(specName, specValue);
                    }
                }
            }

            List<SpecificationInfo> specificationsList = new ArrayList<>();
            for (Map.Entry<String, String> entry : specsMap.entrySet()) {
                specificationsList.add(new SpecificationInfo(entry.getKey(), entry.getValue()));
            }
            product.setSpecifications(specificationsList);

            return product;

        } catch (IOException e) {
            System.err.println("Error scraping Noon product URL " + productUrl + ": " + e.getMessage());
            return null; 
        } catch (NumberFormatException e) {
            System.err.println("Error parsing price for " + productUrl + ": " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error scraping " + productUrl + ": " + e.getMessage());
            return null;
        }
    }

    private static final String LULU_RETAILER_ID = "luluhypermarket";
    private static final String LULU_RETAILER_NAME = "LuLu Hypermarket";

    public ProductDocument scrapeLuluProduct(String productUrl) {
        try {
            Document doc = Jsoup.connect(productUrl)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                .header("Accept-Language", "en-US,en;q=0.9")
                                .header("Accept-Encoding", "gzip, deflate, br")
                                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                                .header("Connection", "keep-alive")
                                .referrer("https://www.google.com/") // Adding a common referrer
                                .ignoreContentType(true) // Add this
                                .timeout(30000) // 30 seconds should be enough if not JS-heavy for initial load
                                .get();
            
            // System.out.println("--- LuLu HTML Content ---"); // Debugging line removed
            // System.out.println(doc.html()); 
            // System.out.println("--- End LuLu HTML Content ---");

            ProductDocument product = new ProductDocument();

            // Product Name
            Element nameElement = doc.selectFirst("h1[data-testid=product-name]");
            if (nameElement != null) {
                product.setName(nameElement.text());
            }

            // Main Image URL
            Element imageElement = doc.selectFirst("div.swiper-slide-active img#product-detail-image");
            if (imageElement != null) {
                product.setImageUrl(imageElement.attr("src"));
            }

            // Description (from product summary points)
            // For LuLu, the description is a list of bullet points. We'll concatenate them.
            Elements summaryPointElements = doc.select("div[data-testid=product-bullet-points] ul li span");
            if (!summaryPointElements.isEmpty()) {
                StringBuilder descriptionBuilder = new StringBuilder();
                for (Element point : summaryPointElements) {
                    descriptionBuilder.append(point.text()).append(". ");
                }
                product.setDescription(descriptionBuilder.toString().trim());
            }


            // Retailer Specific Info
            RetailerInfo luluRetailerInfo = new RetailerInfo();
            luluRetailerInfo.setRetailerId(LULU_RETAILER_ID);
            luluRetailerInfo.setName(LULU_RETAILER_NAME);
            luluRetailerInfo.setProductUrl(productUrl);
            luluRetailerInfo.setInStock(true); // Default assumption
            luluRetailerInfo.setPriceHistory(new ArrayList<>());

            // Current Price
            Element currentPriceElement = doc.selectFirst("span[data-testid=price]");
            if (currentPriceElement != null) {
                String priceText = currentPriceElement.text(); // e.g., "3,599.00 AED"
                String priceStr = priceText.replaceAll("[^\\d.]", "");
                if (!priceStr.isEmpty()) {
                    luluRetailerInfo.setCurrentPrice(Double.parseDouble(priceStr));
                }
                // Currency is part of the text, not stored separately in RetailerInfo model
            }
            
            List<RetailerInfo> retailers = new ArrayList<>();
            retailers.add(luluRetailerInfo);
            product.setRetailers(retailers);

            // Specifications (including Brand and Original Price from summary or dedicated elements)
            Map<String, String> specsMap = new HashMap<>();

            // Brand (try to extract from a dedicated element or product name)
            Element brandLinkElement = doc.selectFirst("div.text-gray-620 > a.text-primary");
            if (brandLinkElement != null && brandLinkElement.text().equalsIgnoreCase("Apple")) { // Example, make more generic
                specsMap.put("Brand", brandLinkElement.text());
            } else if (product.getName() != null && product.getName().toLowerCase().contains("apple")) { // Fallback from name
                 specsMap.put("Brand", "Apple");
            }
            // Add more robust brand extraction if possible

            // Original Price
            Element originalPriceElement = doc.selectFirst("span.text-black\\/50.line-through");
            if (originalPriceElement != null) {
                String originalPriceText = originalPriceElement.text(); // e.g., "4,299.00 AED"
                specsMap.put("Original Price", originalPriceText.trim());
            }
            
            // Product Summary points as specifications
            if (!summaryPointElements.isEmpty()) {
                for (Element point : summaryPointElements) {
                    // Avoid adding redundant "Product Summary" as a spec name
                    // This might need more sophisticated parsing if keys are not explicit
                    specsMap.put("Feature_" + (specsMap.size() - (specsMap.containsKey("Brand") ? 1:0) - (specsMap.containsKey("Original Price") ? 1:0) +1) , point.text());
                }
            }
            // More detailed specifications might require handling dynamically loaded content or specific tabs

            List<SpecificationInfo> specificationsList = new ArrayList<>();
            for (Map.Entry<String, String> entry : specsMap.entrySet()) {
                specificationsList.add(new SpecificationInfo(entry.getKey(), entry.getValue()));
            }
            product.setSpecifications(specificationsList);

            return product;

        } catch (IOException e) {
            System.err.println("Error scraping LuLu product URL " + productUrl + ": " + e.getMessage());
            return null;
        } catch (NumberFormatException e) {
            System.err.println("Error parsing price for LuLu product " + productUrl + ": " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error scraping LuLu product " + productUrl + ": " + e.getMessage());
            return null;
        }
    }
}
