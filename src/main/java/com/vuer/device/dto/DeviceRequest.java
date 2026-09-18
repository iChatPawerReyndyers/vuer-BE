package com.vuer.device.dto;

import com.vuer.device.entity.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceRequest(
        @NotBlank String nickname,
        @NotNull Platform platform,
        String fcmToken
) {}