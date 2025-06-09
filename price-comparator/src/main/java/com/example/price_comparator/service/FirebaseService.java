package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.model.User;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.stereotype.Service;

@Service
public class FirebaseService {

    private final DatabaseReference productsRef;
    private final DatabaseReference usersRef;

    public FirebaseService(FirebaseDatabase firebaseDatabase) {
        this.productsRef = firebaseDatabase.getReference("products");
        this.usersRef = firebaseDatabase.getReference("users");
    }

    public void saveProduct(ProductDocument product) {
        if (product != null && product.getId() != null) {
            try {
                productsRef.child(product.getId()).setValueAsync(product).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveUser(User user) {
        if (user != null) {
            try {
                usersRef.child(user.getUid()).setValueAsync(user).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void test() {
        try {
            usersRef.child("test").setValueAsync("test").get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
