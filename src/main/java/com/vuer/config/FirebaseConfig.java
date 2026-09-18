package com.vuer.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                
                // In production, you would typically pass the path via an environment variable, 
                // e.g., System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
                // For this example, we attempt to load from a default path or classpath.
                // The user must place their service account key at this location.
                
                // Attempting to load from the classpath first
                InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
                
                if (serviceAccount == null) {
                    log.warn("Firebase service account key not found at classpath:firebase-service-account.json. " +
                             "Push notifications will not work until this is configured.");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK successfully initialized.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
        }
    }
}
