package com.example.timetracking.service;

import com.example.timetracking.model.Device;
import com.example.timetracking.model.Location;
import com.example.timetracking.repo.DeviceRepository;
import com.example.timetracking.repo.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final LocationRepository locationRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Device requireActiveRegisteredDevice(String deviceUuid) {
        return deviceRepository.findByDeviceUuidWithLocation(deviceUuid)
            .filter(Device::isActive)
            .orElseThrow(() -> new IllegalStateException("This computer is not registered.") );
    }

    @Transactional
    public Device registerDeviceToLocation(String deviceUuid, long locationId, String computerName) {
        Location location = locationRepository.findById(locationId)
            .orElseThrow(() -> new IllegalArgumentException("Location not found"));

        return deviceRepository.findByDeviceUuid(deviceUuid)
            .map(existing -> {
                existing.setLocation(location);
                existing.setComputerName(computerName);
                existing.setActive(true);
                existing.setRegisteredAt(Instant.now(clock));
                return existing;
            })
            .orElseGet(() -> {
                Device device = new Device();
                device.setDeviceUuid(deviceUuid);
                device.setLocation(location);
                device.setComputerName(computerName);
                device.setActive(true);
                device.setRegisteredAt(Instant.now(clock));
                return deviceRepository.save(device);
            });
    }
}
