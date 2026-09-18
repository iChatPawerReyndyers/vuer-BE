package com.vuer.device.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceNicknameRequest(@NotBlank String nickname) {}