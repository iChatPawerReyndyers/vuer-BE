package com.vuer.device.controller;

import com.vuer.device.dto.DeviceRequest;
import com.vuer.device.dto.DeviceResponse;
import com.vuer.device.service.DeviceService;
import com.vuer.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getDevices(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(deviceService.getUserDevices(user.getId()));
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> registerDevice(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DeviceRequest request) {
        return ResponseEntity.ok(deviceService.registerDevice(user.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeDevice(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        deviceService.removeDevice(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}