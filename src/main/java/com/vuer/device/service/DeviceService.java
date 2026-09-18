package com.vuer.device.service;

import com.vuer.common.exception.DeviceLimitExceededException;
import com.vuer.device.dto.DeviceRequest;
import com.vuer.device.dto.DeviceResponse;
import com.vuer.device.entity.Device;
import com.vuer.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private static final int MAX_DEVICES = 5;

    @Transactional(readOnly = true)
    public List<DeviceResponse> getUserDevices(UUID userId) {
        return deviceRepository.findAllByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeviceResponse registerDevice(UUID userId, DeviceRequest request) {
        long count = deviceRepository.countByUserId(userId);
        if (count >= MAX_DEVICES) {
            throw new DeviceLimitExceededException("Maximum number of devices (" + MAX_DEVICES + ") reached.");
        }

        String generatedToken = UUID.randomUUID().toString();

        Device device = Device.builder()
                .userId(userId)
                .nickname(request.nickname())
                .platform(request.platform())
                .fcmToken(request.fcmToken())
                .deviceToken(generatedToken)
                .isActive(true)
                .build();

        device = deviceRepository.save(device);
        
        return mapToResponse(device);
    }

    @Transactional
    public void removeDevice(UUID userId, UUID deviceId) {
        deviceRepository.deleteByIdAndUserId(deviceId, userId);
    }

    private DeviceResponse mapToResponse(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getNickname(),
                device.getPlatform(),
                device.isActive(),
                device.getRegisteredAt(),
                device.getDeviceToken()
        );
    }
}