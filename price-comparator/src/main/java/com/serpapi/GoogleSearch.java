package com.serpapi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public class GoogleSearch {

    private final Map<String, String> parameter;

    public GoogleSearch(Map<String, String> parameter) {
        this.parameter = parameter;
    }

    public JsonObject getJson() throws SerpApiSearchException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            URIBuilder builder = new URIBuilder("https://serpapi.com/search.json");
            for (Map.Entry<String, String> entry : parameter.entrySet()) {
                builder.addParameter(entry.getKey(), entry.getValue());
            }
            HttpGet request = new HttpGet(builder.build());
            request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");

            try (var response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    throw new SerpApiSearchException("API request failed with code: " + statusCode);
                }
                String responseBody = EntityUtils.toString(response.getEntity());
                return new Gson().fromJson(responseBody, JsonObject.class);
            }
        } catch (IOException | URISyntaxException | JsonSyntaxException | org.apache.hc.core5.http.ParseException e) {
            throw new SerpApiSearchException(e);
        }
    }
}