package com.vuer.message.dto;

import com.vuer.message.entity.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record MessageIngestRequest(
        @NotBlank String deviceToken,
        @NotBlank String senderAddress,
        @NotBlank String body,
        @NotNull ChannelType channelType,
        String subject,
        @NotNull LocalDateTime originalTimestamp
) {}