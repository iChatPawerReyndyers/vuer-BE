package com.vuer.message.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String senderIdentity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType channelType;

    private String displayName;

    private LocalDateTime lastMessageAt;
}