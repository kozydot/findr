package com.example.price_comparator.service;

import com.example.price_comparator.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseService firebaseService;

    public AuthService(FirebaseAuth firebaseAuth, FirebaseService firebaseService) {
        this.firebaseAuth = firebaseAuth;
        this.firebaseService = firebaseService;
    }

    public UserRecord registerUser(User user) throws FirebaseAuthException {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(user.getEmail())
                .setPassword(user.getPassword())
                .setDisplayName(user.getDisplayName())
                .setDisabled(false);

        UserRecord userRecord = firebaseAuth.createUser(request);
        User newUser = new User();
        newUser.setUid(userRecord.getUid());
        newUser.setEmail(userRecord.getEmail());
        newUser.setDisplayName(userRecord.getDisplayName());
        firebaseService.saveUser(newUser);
        return userRecord;
    }

    public String loginUser(User user) throws FirebaseAuthException {
        // This is a simplified login mechanism. In a real application, you would
        // typically verify the user's password with Firebase Authentication.
        // Since we don't have direct password verification with the Admin SDK,
        // this example will just create a custom token for the user.
        // The frontend would then use this token to sign in with the Firebase client SDK.
        UserRecord userRecord = firebaseAuth.getUserByEmail(user.getEmail());
        return firebaseAuth.createCustomToken(userRecord.getUid());
    }

    public void test() {
        firebaseService.test();
    }
}
