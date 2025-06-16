package com.example.price_comparator.model;

import lombok.Data;

import java.util.List;

@Data
public class User {
    private String uid;
    private String email;
    private String password; // This will be used for registration and then discarded
    private String displayName;
    private List<String> bookmarks;
}
