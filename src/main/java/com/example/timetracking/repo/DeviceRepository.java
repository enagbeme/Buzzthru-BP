package com.example.timetracking.repo;

import com.example.timetracking.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceUuid(String deviceUuid);

    @Query("select d from Device d join fetch d.location where d.deviceUuid = :deviceUuid")
    Optional<Device> findByDeviceUuidWithLocation(@Param("deviceUuid") String deviceUuid);
}
