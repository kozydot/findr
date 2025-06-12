package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
            productsRef.child(product.getId()).setValueAsync(product);
        }
    }

    public Optional<ProductDocument> getProduct(String id) {
        CompletableFuture<Optional<ProductDocument>> future = new CompletableFuture<>();
        productsRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ProductDocument product = dataSnapshot.getValue(ProductDocument.class);
                future.complete(Optional.ofNullable(product));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
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
