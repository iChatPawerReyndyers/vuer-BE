package com.vuer.email.dto;

public record LinkEmailRequest(
        String imapHost,
        int imapPort,
        String username,
        String password,
        String displayLabel
) {}
