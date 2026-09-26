package com.vuer.message.service;

import com.vuer.common.exception.ResourceNotFoundException;
import com.vuer.message.entity.ChannelType;
import com.vuer.message.entity.Conversation;
import com.vuer.message.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    @Transactional
    public Conversation findOrCreate(UUID userId, String senderAddress, ChannelType channelType, LocalDateTime timestamp) {
        String normalizedIdentity = normalizeIdentity(senderAddress, channelType);

        return conversationRepository.findByUserIdAndSenderIdentityAndChannelType(userId, normalizedIdentity, channelType)
                .map(conversation -> {
                    if (conversation.getLastMessageAt() == null || timestamp.isAfter(conversation.getLastMessageAt())) {
                        conversation.setLastMessageAt(timestamp);
                        return conversationRepository.save(conversation);
                    }
                    return conversation;
                })
                .orElseGet(() -> {
                    Conversation newConv = Conversation.builder()
                            .userId(userId)
                            .senderIdentity(normalizedIdentity)
                            .channelType(channelType)
                            .displayName(senderAddress)
                            .lastMessageAt(timestamp)
                            .build();
                    return conversationRepository.save(newConv);
                });
    }

    /**
     * Renames the conversation for a given sender - this is what lets a
     * contact nickname (set client-side in FilterRulesScreen) actually show
     * up in push notification titles, since those are composed server-side
     * from Conversation.displayName.
     *
     * Only affects a conversation that already exists (i.e. at least one
     * message has been received from this sender) - there's nothing to
     * rename otherwise, and the nickname will simply not have taken effect
     * yet for a sender you haven't gotten a message from.
     */
    @Transactional
    public Conversation updateDisplayNameBySender(UUID userId, String senderAddress, ChannelType channelType, String displayName) {
        String normalizedIdentity = normalizeIdentity(senderAddress, channelType);
        Conversation conversation = conversationRepository
                .findByUserIdAndSenderIdentityAndChannelType(userId, normalizedIdentity, channelType)
                .orElseThrow(() -> new ResourceNotFoundException("No conversation yet for this sender"));

        conversation.setDisplayName(displayName);
        return conversationRepository.save(conversation);
    }

    private String normalizeIdentity(String senderAddress, ChannelType channelType) {
        if (senderAddress == null || senderAddress.isBlank()) {
            return "Unknown";
        }
        if (channelType == ChannelType.SMS) {
            String digits = senderAddress.replaceAll("[^0-9+]", "");
            return digits.isEmpty() ? senderAddress.trim() : digits;
        }
        return senderAddress.toLowerCase().trim();
    }
}