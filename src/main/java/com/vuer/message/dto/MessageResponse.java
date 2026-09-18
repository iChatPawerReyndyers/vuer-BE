package com.vuer.message.dto;

import com.vuer.message.entity.ChannelType;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        String senderAddress,
        String body,
        ChannelType channelType,
        String subject,
        String deviceNickname,
        LocalDateTime originalTimestamp
) {}