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
        Device device = deviceRepository.findByUserIdAndDeviceToken(userId, request.deviceToken())
                .orElseGet(() -> {
                    if (deviceRepository.countByUserId(userId) >= MAX_DEVICES) {
                        throw new DeviceLimitExceededException("Maximum number of devices (" + MAX_DEVICES + ") reached.");
                    }
                    return Device.builder()
                            .userId(userId)
                            .deviceToken(request.deviceToken())
                            .isActive(true)
                            .build();
                });

        device.setPlatform(request.platform());
        device.setModel(request.model());
        device.setFcmToken(request.fcmToken());
        device.setActive(true);
        if (device.getNickname() == null || device.getNickname().isBlank()) {
            device.setNickname(request.nickname());
        }

        device = deviceRepository.save(device);
        
        return mapToResponse(device);
    }

    @Transactional
    public void removeDevice(UUID userId, UUID deviceId) {
        deviceRepository.deleteByIdAndUserId(deviceId, userId);
    }

    @Transactional
    public DeviceResponse updateNickname(UUID userId, UUID deviceId, String nickname) {
        Device device = deviceRepository.findById(deviceId)
                .filter(item -> item.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));
        device.setNickname(nickname.trim());
        return mapToResponse(deviceRepository.save(device));
    }

    private DeviceResponse mapToResponse(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getNickname(),
                device.getModel(),
                device.getPlatform(),
                device.isActive(),
                device.getRegisteredAt(),
                device.getDeviceToken()
        );
    }
}