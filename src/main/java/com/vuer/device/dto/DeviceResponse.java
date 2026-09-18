package com.vuer.device.dto;

import com.vuer.device.entity.Platform;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        String nickname,
        String model,
        Platform platform,
        boolean isActive,
        LocalDateTime registeredAt,
        String deviceToken
) {}