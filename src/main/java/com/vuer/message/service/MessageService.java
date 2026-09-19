package com.vuer.message.service;

import com.vuer.common.exception.ResourceNotFoundException;
import com.vuer.device.entity.Device;
import com.vuer.device.repository.DeviceRepository;
import com.vuer.message.dto.MessageIngestRequest;
import com.vuer.message.dto.MessageResponse;
import com.vuer.message.entity.Conversation;
import com.vuer.message.entity.Message;
import com.vuer.message.repository.ConversationRepository;
import com.vuer.message.repository.MessageRepository;
import com.vuer.notification.service.PushNotificationService;
import com.vuer.websocket.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final DeviceRepository deviceRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final PushNotificationService pushNotificationService;

    @Transactional
    public void ingestMessage(MessageIngestRequest request) {
        Device device = deviceRepository.findByDeviceToken(request.deviceToken())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        Conversation conversation = conversationService.findOrCreate(
                device.getUserId(),
                request.senderAddress(),
                request.channelType(),
                request.originalTimestamp()
        );

        Message message = Message.builder()
                .conversationId(conversation.getId())
                .deviceId(device.getId())
                .senderAddress(request.senderAddress())
                .bodyEncrypted(request.body())
                .channelType(request.channelType())
                .subject(request.subject())
                .originalTimestamp(request.originalTimestamp())
                .build();

        Message savedMessage = messageRepository.save(message);

        MessageResponse response = new MessageResponse(
                savedMessage.getId(),
                conversation.getId(),
                savedMessage.getSenderAddress(),
                conversation.getSenderIdentity(),
                savedMessage.getBodyEncrypted(),
                savedMessage.getChannelType(),
                savedMessage.getSubject(),
                device.getNickname(),
                savedMessage.getOriginalTimestamp()
        );

        webSocketNotificationService.notifyUser(device.getUserId(), response);
        pushNotificationService.sendPushNotification(device.getUserId(), device.getId(), response);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getConversationMessages(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Conversation not found");
        }

        return messageRepository.findByConversationIdOrderByOriginalTimestampDesc(conversationId).stream()
                .map(msg -> {
                    Device device = deviceRepository.findById(msg.getDeviceId()).orElse(null);
                    return new MessageResponse(
                            msg.getId(),
                            conversation.getId(),
                            msg.getSenderAddress(),
                            conversation.getSenderIdentity(),
                            msg.getBodyEncrypted(),
                            msg.getChannelType(),
                            msg.getSubject(),
                            device != null ? device.getNickname() : "Unknown",
                            msg.getOriginalTimestamp()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> searchMessages(UUID userId, String query) {
        // Since messages are encrypted at rest, we need to load conversations for the user,
        // fetch their messages, and filter in memory after decryption
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByLastMessageAtDesc(userId);

        if (conversations.isEmpty()) {
            return List.of();
        }

        Map<UUID, Conversation> conversationsById = conversations.stream()
                .collect(Collectors.toMap(Conversation::getId, Function.identity()));

        String lowerQuery = query.toLowerCase();
        return messageRepository.findAllByConversationIdIn(new ArrayList<>(conversationsById.keySet())).stream()
                .filter(msg -> {
                    // bodyEncrypted is auto-decrypted by the JPA AttributeConverter when read
                    String body = msg.getBodyEncrypted() != null ? msg.getBodyEncrypted().toLowerCase() : "";
                    String sender = msg.getSenderAddress() != null ? msg.getSenderAddress().toLowerCase() : "";
                    String subject = msg.getSubject() != null ? msg.getSubject().toLowerCase() : "";
                    return body.contains(lowerQuery) || sender.contains(lowerQuery) || subject.contains(lowerQuery);
                })
                .map(msg -> {
                    Device device = deviceRepository.findById(msg.getDeviceId()).orElse(null);
                    Conversation conversation = conversationsById.get(msg.getConversationId());
                    return new MessageResponse(
                            msg.getId(),
                            msg.getConversationId(),
                            msg.getSenderAddress(),
                            conversation != null ? conversation.getSenderIdentity() : msg.getSenderAddress(),
                            msg.getBodyEncrypted(),
                            msg.getChannelType(),
                            msg.getSubject(),
                            device != null ? device.getNickname() : "Unknown",
                            msg.getOriginalTimestamp()
                    );
                })
                .limit(50)
                .collect(Collectors.toList());
    }
}