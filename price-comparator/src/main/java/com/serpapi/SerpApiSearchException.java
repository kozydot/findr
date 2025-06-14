package com.serpapi;

public class SerpApiSearchException extends Exception {

    public SerpApiSearchException(String message) {
        super(message);
    }

    public SerpApiSearchException(Throwable cause) {
        super(cause);
    }
}