package com.vuer.email.entity;

import com.vuer.common.crypto.AesEncryptor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores per-user IMAP credentials so the email polling service
 * knows which inboxes to poll and how to authenticate.
 */
@Entity
@Table(name = "linked_email_accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedEmailAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "imap_host", nullable = false)
    private String imapHost;

    @Column(name = "imap_port", nullable = false)
    private int imapPort;

    @Column(name = "username", nullable = false)
    private String username;

    /** Stored encrypted at rest using AES-256 */
    @Convert(converter = AesEncryptor.class)
    @Column(name = "encrypted_password", nullable = false)
    private String encryptedPassword;

    @Column(name = "display_label")
    private String displayLabel;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isActive = true;
    }
}
