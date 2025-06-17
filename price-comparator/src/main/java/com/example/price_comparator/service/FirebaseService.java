package com.example.price_comparator.service;

import com.example.price_comparator.model.ProductDocument;
import com.example.price_comparator.model.SpecificationInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class FirebaseService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseService.class);
    private final FirebaseDatabase database;

    public FirebaseService(FirebaseDatabase database) {
        this.database = database;
    }

    private String sanitizeKey(String key) {
        return key.replace(".", "").replace("/", "").replace("#", "").replace("$", "").replace("[", "").replace("]", "");
    }

    public void saveProduct(ProductDocument product) {
        if (product == null || product.getId() == null) {
            logger.error("Cannot save a null product or a product with a null ID.");
            return;
        }

        // Sanitize specification keys
        if (product.getSpecifications() != null) {
            List<SpecificationInfo> sanitizedSpecs = new ArrayList<>();
            for (SpecificationInfo spec : product.getSpecifications()) {
                String sanitizedName = sanitizeKey(spec.getName());
                sanitizedSpecs.add(new SpecificationInfo(sanitizedName, spec.getValue()));
            }
            product.setSpecifications(sanitizedSpecs);
        }

        DatabaseReference ref = database.getReference("products/" + product.getId());
        ref.setValueAsync(product);
    }
    
    public CompletableFuture<ProductDocument> getProduct(String id) {
        DatabaseReference ref = database.getReference("products/" + id);
        CompletableFuture<ProductDocument> future = new CompletableFuture<>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                future.complete(dataSnapshot.getValue(ProductDocument.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    public CompletableFuture<List<ProductDocument>> getAllProducts() {
        DatabaseReference ref = database.getReference("products");
        CompletableFuture<List<ProductDocument>> future = new CompletableFuture<>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<ProductDocument> products = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    products.add(snapshot.getValue(ProductDocument.class));
                }
                future.complete(products);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future.orTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
    }

    public CompletableFuture<List<ProductDocument>> searchProductsByName(String query) {
        DatabaseReference ref = database.getReference("products");
        CompletableFuture<List<ProductDocument>> future = new CompletableFuture<>();
        String lowerCaseQuery = query.toLowerCase();

        ref.orderByChild("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<ProductDocument> products = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ProductDocument product = snapshot.getValue(ProductDocument.class);
                    if (product != null && product.getName() != null && product.getName().toLowerCase().contains(lowerCaseQuery)) {
                        products.add(product);
                    }
                }
                future.complete(products);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });

        return future.orTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
    }

    public CompletableFuture<Boolean> productsExist() {
        DatabaseReference ref = database.getReference("products");
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                future.complete(dataSnapshot.exists() && dataSnapshot.hasChildren());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future.orTimeout(10, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void saveUser(com.example.price_comparator.model.User user) {
        if (user == null || user.getUid() == null) {
            logger.error("Cannot save a null user or a user with a null UID.");
            return;
        }
        DatabaseReference ref = database.getReference("users/" + user.getUid());
        ref.setValueAsync(user);
    }

    public void deleteUser(String uid) {
        DatabaseReference ref = database.getReference("users/" + uid);
        ref.removeValueAsync();
    }

    public void addBookmark(String userId, String productId) {
        DatabaseReference ref = database.getReference("users/" + userId + "/bookmarks");
        ref.child(productId).setValueAsync(true);
    }

    public void removeBookmark(String userId, String productId) {
        DatabaseReference ref = database.getReference("users/" + userId + "/bookmarks/" + productId);
        ref.removeValueAsync();
    }

    public CompletableFuture<List<String>> getBookmarks(String userId) {
        DatabaseReference ref = database.getReference("users/" + userId + "/bookmarks");
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> bookmarks = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    bookmarks.add(snapshot.getKey());
                }
                future.complete(bookmarks);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    public CompletableFuture<Long> getLastAmazonFetchTimestamp() {
        DatabaseReference ref = database.getReference("metadata/lastAmazonFetch");
        CompletableFuture<Long> future = new CompletableFuture<>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long timestamp = dataSnapshot.getValue(Long.class);
                future.complete(timestamp != null ? timestamp : 0L);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future.orTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void updateLastAmazonFetchTimestamp(long timestamp) {
        DatabaseReference ref = database.getReference("metadata/lastAmazonFetch");
        ref.setValueAsync(timestamp);
    }
}
