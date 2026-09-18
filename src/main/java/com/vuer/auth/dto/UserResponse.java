package com.vuer.auth.dto;

import com.vuer.user.entity.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String displayName
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName());
    }
}