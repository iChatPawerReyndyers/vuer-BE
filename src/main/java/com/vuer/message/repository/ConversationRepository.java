package com.vuer.message.repository;

import com.vuer.message.entity.ChannelType;
import com.vuer.message.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByUserIdOrderByLastMessageAtDesc(UUID userId);
    Optional<Conversation> findByUserIdAndSenderIdentityAndChannelType(UUID userId, String senderIdentity, ChannelType channelType);
}