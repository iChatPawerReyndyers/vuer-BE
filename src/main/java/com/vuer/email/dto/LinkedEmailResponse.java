package com.vuer.email.dto;

import java.util.UUID;

public record LinkedEmailResponse(
        UUID id,
        String displayLabel,
        String username,
        String imapHost,
        int imapPort,
        boolean isActive
) {}
