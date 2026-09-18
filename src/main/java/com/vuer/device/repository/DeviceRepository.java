package com.vuer.device.repository;

import com.vuer.device.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findAllByUserId(UUID userId);
    long countByUserId(UUID userId);
    Optional<Device> findByDeviceToken(String deviceToken);
    void deleteByIdAndUserId(UUID id, UUID userId);
}