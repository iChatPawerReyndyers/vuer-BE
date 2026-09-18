package com.vuer.websocket.service;

import com.vuer.message.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyUser(UUID userId, MessageResponse message) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/messages",
                message
        );
    }
}