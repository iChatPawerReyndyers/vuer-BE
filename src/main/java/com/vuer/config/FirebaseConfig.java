package com.vuer.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                    String cleaned = sanitizeJson(envJson);
                    serviceAccount = new ByteArrayInputStream(cleaned.getBytes(StandardCharsets.UTF_8));
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

                // Buffer the credential bytes so we can both log which account
                // was loaded (helps catch "used the wrong project's key" or a
                // stale/revoked key at a glance) and hand them to Google's SDK,
                // without consuming the stream twice.
                byte[] credentialBytes = serviceAccount.readAllBytes();
                logServiceAccountIdentity(credentialBytes);

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(credentialBytes)))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK successfully initialized.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
        }
    }

    /**
     * Strips common copy-paste artifacts from a pasted env var value:
     * surrounding whitespace, and a single pair of wrapping quotes that
     * sometimes get added when a JSON value is pasted into a dashboard
     * text field that assumes a plain string.
     */
    private String sanitizeJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.length() >= 2
                && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * Logs the service account's client_email and project_id (safe, public
     * fields) so it's obvious at startup which credential is active - e.g.
     * to spot a stale key or a key from the wrong Firebase project without
     * needing to wait for a push notification to fail first. Never logs the
     * private_key itself.
     */
    private void logServiceAccountIdentity(byte[] credentialBytes) {
        try {
            JsonNode node = new ObjectMapper().readTree(credentialBytes);
            String clientEmail = node.path("client_email").asText("<missing>");
            String projectId = node.path("project_id").asText("<missing>");
            log.info("Firebase service account loaded: client_email={}, project_id={}", clientEmail, projectId);
        } catch (Exception e) {
            log.warn("Could not parse Firebase credential JSON to log its identity: {}", e.getMessage());
        }
    }
}