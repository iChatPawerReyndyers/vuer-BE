package com.vuer.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.vuer.device.entity.Device;
import com.vuer.device.repository.DeviceRepository;
import com.vuer.message.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final DeviceRepository deviceRepository;

    public void sendPushNotification(UUID userId, UUID originDeviceId, MessageResponse messageResponse) {
        List<Device> devices = deviceRepository.findAllByUserId(userId);
        
        for (Device device : devices) {
            if (!device.getId().equals(originDeviceId) && device.getFcmToken() != null && device.isActive()) {
                try {
                    String title = "New Message: " + messageResponse.senderAddress();
                    String preview = messageResponse.body() != null && messageResponse.body().length() > 50 
                            ? messageResponse.body().substring(0, 50) + "..." 
                            : messageResponse.body();
                            
                    Message message = Message.builder()
                            .setToken(device.getFcmToken())
                            .setNotification(Notification.builder()
                                    .setTitle(title)
                                    .setBody(preview)
                                    .build())
                            .putData("messageId", messageResponse.id().toString())
                            .build();

                    String response = FirebaseMessaging.getInstance().send(message);
                    log.info("Successfully sent push notification to device {}: {}", device.getNickname(), response);
                } catch (Exception e) {
                    log.error("Error sending push notification to device {}: {}", device.getNickname(), e.getMessage(), e);
                }
            }
        }
    }
}