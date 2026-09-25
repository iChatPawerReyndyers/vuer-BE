package com.vuer.notification.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.vuer.device.entity.Device;
import com.vuer.device.repository.DeviceRepository;
import com.vuer.message.dto.MessageResponse;
import com.vuer.message.entity.ChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private static final String SMS_ICON = "ic_notification_sms";
    private static final String EMAIL_ICON = "ic_notification_email";
    private static final String SMS_COLOR = "#FF9500";
    private static final String EMAIL_COLOR = "#007AFF";
    private static final String CHANNEL_ID = "vuer_messages";

    private final DeviceRepository deviceRepository;

    public void sendPushNotification(UUID userId, UUID originDeviceId, MessageResponse messageResponse) {
        List<Device> devices = deviceRepository.findAllByUserId(userId);

        boolean isEmail = messageResponse.channelType() == ChannelType.EMAIL;
        String icon = isEmail ? EMAIL_ICON : SMS_ICON;
        String color = isEmail ? EMAIL_COLOR : SMS_COLOR;

        // Every active, linked device gets notified - including the device that
        // physically received the SMS/email and forwarded it (originDeviceId).
        //
        // A device that registered before push permission was granted has its
        // fcmToken stored as an empty string, not null - Firebase's Message
        // builder rejects a blank token with "Exactly one of token, topic or
        // condition must be specified", so this must check for blank, not just null.
        for (Device device : devices) {
            if (device.getFcmToken() != null && !device.getFcmToken().isBlank() && device.isActive()) {
                try {
                    // Display the sender address / email as the title directly, matching the custom notification UI design
                    String title = messageResponse.senderAddress() != null ? messageResponse.senderAddress() : "Vuer";
                    String preview = messageResponse.body() != null && messageResponse.body().length() > 100
                            ? messageResponse.body().substring(0, 100) + "..."
                            : (messageResponse.body() != null ? messageResponse.body() : "");

                    Message message = Message.builder()
                            .setToken(device.getFcmToken())
                            .setNotification(Notification.builder()
                                    .setTitle(title)
                                    .setBody(preview)
                                    .build())
                            .setAndroidConfig(AndroidConfig.builder()
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .setNotification(AndroidNotification.builder()
                                            .setChannelId(CHANNEL_ID)
                                            .setIcon(icon)
                                            .setColor(color)
                                            .setDefaultSound(true)
                                            .setDefaultVibrateTimings(true)
                                            .setPriority(AndroidNotification.Priority.HIGH)
                                            .build())
                                    .build())
                            .putData("messageId", messageResponse.id() != null ? messageResponse.id().toString() : "")
                            .putData("conversationId", messageResponse.conversationId() != null ? messageResponse.conversationId().toString() : "")
                            .putData("senderAddress", messageResponse.senderAddress() != null ? messageResponse.senderAddress() : "")
                            .putData("body", messageResponse.body() != null ? messageResponse.body() : "")
                            .putData("channelType", messageResponse.channelType() != null ? messageResponse.channelType().name() : "SMS")
                            .putData("originalTimestamp", messageResponse.originalTimestamp() != null ? messageResponse.originalTimestamp() : "")
                            .putData("deviceNickname", messageResponse.deviceNickname() != null ? messageResponse.deviceNickname() : "")
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