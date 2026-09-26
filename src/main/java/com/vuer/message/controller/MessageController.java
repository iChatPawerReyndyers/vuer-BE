package com.vuer.message.controller;

import com.vuer.device.entity.Device;
import com.vuer.device.repository.DeviceRepository;
import com.vuer.message.dto.ConversationResponse;
import com.vuer.message.dto.MessageIngestRequest;
import com.vuer.message.dto.MessageResponse;
import com.vuer.message.dto.UpdateDisplayNameRequest;
import com.vuer.message.entity.Message;
import com.vuer.message.repository.ConversationRepository;
import com.vuer.message.repository.MessageRepository;
import com.vuer.message.service.ConversationService;
import com.vuer.message.service.MessageService;
import com.vuer.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final DeviceRepository deviceRepository;

    @PostMapping("/messages/ingest")
    public ResponseEntity<Void> ingestMessage(@Valid @RequestBody MessageIngestRequest request) {
        messageService.ingestMessage(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations(@AuthenticationPrincipal User user) {
        List<ConversationResponse> responses = conversationRepository.findByUserIdOrderByLastMessageAtDesc(user.getId())
                .stream()
                .map(c -> {
                    // Fetch last message for preview and device nickname
                    String lastPreview = "";
                    String deviceNickname = "";
                    var latestMessage = messageRepository.findTopByConversationIdOrderByOriginalTimestampDesc(c.getId());
                    if (latestMessage.isPresent()) {
                        Message msg = latestMessage.get();
                        lastPreview = msg.getBodyEncrypted() != null
                                ? (msg.getBodyEncrypted().length() > 80
                                   ? msg.getBodyEncrypted().substring(0, 80) + "..."
                                   : msg.getBodyEncrypted())
                                : "";
                        Device device = deviceRepository.findById(msg.getDeviceId()).orElse(null);
                        deviceNickname = device != null ? device.getNickname() : "Unknown";
                    }
                    long count = messageRepository.countByConversationId(c.getId());

                    return new ConversationResponse(
                            c.getId(),
                            c.getSenderIdentity(),
                            c.getChannelType(),
                            c.getDisplayName(),
                            c.getLastMessageAt(),
                            lastPreview,
                            deviceNickname,
                            count
                    );
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(messageService.getConversationMessages(id, user.getId()));
    }

    @GetMapping("/messages/search")
    public ResponseEntity<List<MessageResponse>> searchMessages(
            @AuthenticationPrincipal User user,
            @RequestParam String q) {
        return ResponseEntity.ok(messageService.searchMessages(user.getId(), q));
    }

    @PatchMapping("/conversations/display-name")
    public ResponseEntity<Void> updateDisplayName(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateDisplayNameRequest request) {
        conversationService.updateDisplayNameBySender(
                user.getId(),
                request.senderAddress(),
                request.channelType(),
                request.displayName()
        );
        return ResponseEntity.noContent().build();
    }
}