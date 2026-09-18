package com.vuer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {
    
    @Value("${app.encryption.key}")
    private String encryptionKey;
    
    @Bean
    public String aesEncryptionKey() {
        return encryptionKey;
    }
}