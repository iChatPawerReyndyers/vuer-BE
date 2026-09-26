package com.vuer.message.dto;

import com.vuer.message.entity.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Body for PATCH /api/conversations/display-name - renames the conversation
 * for a given sender (i.e. sets the nickname used in the feed and in push
 * notification titles), identified by sender address + channel type rather
 * than a conversation UUID, since the client's contact rules are keyed by
 * sender, not by conversation id.
 */
public record UpdateDisplayNameRequest(
        @NotBlank String senderAddress,
        @NotNull ChannelType channelType,
        @NotBlank String displayName
) {}