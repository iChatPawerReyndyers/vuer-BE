package com.vuer.message.service;

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