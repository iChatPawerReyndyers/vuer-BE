package com.vuer.message.entity;

import com.vuer.common.crypto.AesEncryptor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID conversationId;

    @Column(nullable = false)
    private UUID deviceId;

    @Column(nullable = false)
    private String senderAddress;

    @Convert(converter = AesEncryptor.class)
    @Column(nullable = false, columnDefinition = "text")
    private String bodyEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType channelType;

    private String subject;

    @Column(nullable = false)
    private LocalDateTime originalTimestamp;

    private LocalDateTime ingestedAt;

    @PrePersist
    protected void onCreate() {
        ingestedAt = LocalDateTime.now();
    }
}