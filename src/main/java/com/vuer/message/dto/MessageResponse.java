package com.vuer.message.dto;

import com.vuer.message.entity.ChannelType;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        String senderAddress,
        String senderIdentity,
        String body,
        ChannelType channelType,
        String subject,
        String deviceNickname,
        LocalDateTime originalTimestamp
) {}