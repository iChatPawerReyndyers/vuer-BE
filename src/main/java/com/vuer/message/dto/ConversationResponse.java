package com.vuer.message.dto;

import com.vuer.message.entity.ChannelType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        String senderIdentity,
        ChannelType channelType,
        String displayName,
        LocalDateTime lastMessageAt,
        String lastMessagePreview,
        String deviceNickname,
        long messageCount
) {}