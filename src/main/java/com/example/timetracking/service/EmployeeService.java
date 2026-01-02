package com.example.timetracking.service;

import com.example.timetracking.model.Employee;
import com.example.timetracking.model.EmployeeRole;
import com.example.timetracking.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PinService pinService;
    private final Clock clock;

    @Transactional
    public CreatedEmployee createEmployee(String fullName, EmployeeRole role) {
        String pin = pinService.generatePin();

        Employee e = new Employee();
        e.setFullName(fullName);
        e.setRole(role);
        e.setActive(true);
        e.setCreatedAt(Instant.now(clock));
        e.setPinHash(pinService.hashPin(pin));

        employeeRepository.save(e);
        return new CreatedEmployee(e, pin);
    }

    @Transactional
    public String resetPin(long employeeId) {
        Employee e = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        String pin = pinService.generatePin();
        e.setPinHash(pinService.hashPin(pin));
        return pin;
    }

    public record CreatedEmployee(Employee employee, String rawPin) {}
}
