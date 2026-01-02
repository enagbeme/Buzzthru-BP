package com.example.timetracking.service;

import com.example.timetracking.model.Device;
import com.example.timetracking.model.Employee;
import com.example.timetracking.repo.EmployeeRepository;
import com.example.timetracking.repo.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TimeClockService {

    private final EmployeeRepository employeeRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final PinService pinService;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ClockResult clockIn(Device device, String pin) {
        Employee employee = findActiveEmployeeByPin(pin);

        timeEntryRepository.findFirstByEmployeeAndClockOutTimeIsNull(employee)
            .ifPresent(open -> {
                throw new IllegalStateException("You are already clocked in.");
            });

        var entry = new com.example.timetracking.model.TimeEntry();
        entry.setEmployee(employee);
        entry.setDevice(device);
        entry.setLocation(device.getLocation());
        entry.setClockInTime(Instant.now(clock));
        entry.setCreatedAt(Instant.now(clock));
        timeEntryRepository.save(entry);

        eventPublisher.publishEvent(new TimeClockEvent(device.getLocation().getId()));

        return new ClockResult(employee.getFullName(), entry.getClockInTime());
    }

    @Transactional
    public ClockOutResult clockOut(Device device, String pin) {
        Employee employee = findActiveEmployeeByPin(pin);

        var open = timeEntryRepository.findFirstByEmployeeAndClockOutTimeIsNull(employee)
            .orElseThrow(() -> new IllegalStateException("No open shift found.") );

        if (!open.getDevice().getId().equals(device.getId())) {
            throw new IllegalStateException("You must clock out at the same location.");
        }

        Instant out = Instant.now(clock);
        open.setClockOutTime(out);

        eventPublisher.publishEvent(new TimeClockEvent(device.getLocation().getId()));

        return new ClockOutResult(employee.getFullName(), open.getClockInTime(), out);
    }

    private Employee findActiveEmployeeByPin(String pin) {
        if (pin == null || pin.isBlank()) {
            throw new IllegalArgumentException("PIN is required");
        }

        for (Employee e : employeeRepository.findAll()) {
            if (e.isActive() && pinService.matches(pin, e.getPinHash())) {
                return e;
            }
        }

        throw new IllegalStateException("Invalid PIN");
    }

    public record ClockResult(String employeeName, Instant clockInTime) {}

    public record ClockOutResult(String employeeName, Instant clockInTime, Instant clockOutTime) {}
}
