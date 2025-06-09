package com.example.price_comparator.config;

import com.example.price_comparator.service.FirebaseService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class TestFirebaseConfig {

    @Bean
    @Primary
    public FirebaseDatabase firebaseDatabase() {
        return Mockito.mock(FirebaseDatabase.class);
    }

    @Bean
    @Primary
    public FirebaseService firebaseService() {
        return Mockito.mock(FirebaseService.class);
    }

    @Bean
    @Primary
    public FirebaseAuth firebaseAuth() {
        return Mockito.mock(FirebaseAuth.class);
    }
}
