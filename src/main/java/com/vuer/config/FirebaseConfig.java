package com.vuer.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = null;

                // 1. Check raw JSON string in environment variable (Render environment variable)
                String envJson = System.getenv("FIREBASE_CREDENTIALS_JSON");
                if (envJson != null && !envJson.isBlank()) {
                    log.info("Loading Firebase credentials from FIREBASE_CREDENTIALS_JSON environment variable");
                    serviceAccount = new ByteArrayInputStream(envJson.getBytes(StandardCharsets.UTF_8));
                }

                // 2. Check file path from GOOGLE_APPLICATION_CREDENTIALS (Render Secret File)
                if (serviceAccount == null) {
                    String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                    if (credentialsPath != null && !credentialsPath.isBlank()) {
                        File file = new File(credentialsPath);
                        if (file.exists()) {
                            log.info("Loading Firebase credentials from GOOGLE_APPLICATION_CREDENTIALS: {}", credentialsPath);
                            serviceAccount = new FileInputStream(file);
                        }
                    }
                }

                // 3. Fallback to classpath resource (local development)
                if (serviceAccount == null) {
                    serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
                    if (serviceAccount != null) {
                        log.info("Loading Firebase credentials from classpath:firebase-service-account.json");
                    }
                }

                if (serviceAccount == null) {
                    log.warn("Firebase credentials not found (checked FIREBASE_CREDENTIALS_JSON, GOOGLE_APPLICATION_CREDENTIALS, and classpath). Push notifications will be disabled.");
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
