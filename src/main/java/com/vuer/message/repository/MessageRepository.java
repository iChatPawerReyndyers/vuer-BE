package com.vuer.message.repository;

import com.vuer.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationIdOrderByOriginalTimestampDesc(UUID conversationId);

    Optional<Message> findTopByConversationIdOrderByOriginalTimestampDesc(UUID conversationId);

    long countByConversationId(UUID conversationId);

    List<Message> findAllByConversationIdIn(List<UUID> conversationIds);
}