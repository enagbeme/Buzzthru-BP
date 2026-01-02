package com.example.timetracking.bootstrap;

import com.example.timetracking.model.EmployeeRole;
import com.example.timetracking.model.Location;
import com.example.timetracking.model.LocationType;
import com.example.timetracking.repo.EmployeeRepository;
import com.example.timetracking.repo.LocationRepository;
import com.example.timetracking.service.EmployeeService;
import com.example.timetracking.service.PinService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class BootstrapData implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final LocationRepository locationRepository;
    private final EmployeeService employeeService;
    private final PinService pinService;
    private final Clock clock;

    @Value("${app.bootstrap.admin-pin:}")
    private String bootstrapAdminPin;

    @Override
    public void run(String... args) {
        if (locationRepository.count() == 0) {
            Location l1 = new Location();
            l1.setName("Buzzthru Laundromat 1");
            l1.setType(LocationType.LAUNDRY);
            l1.setActive(true);
            locationRepository.save(l1);

            Location l2 = new Location();
            l2.setName("Buzzthru Laundromat 2");
            l2.setType(LocationType.LAUNDRY);
            l2.setActive(true);
            locationRepository.save(l2);

            Location g = new Location();
            g.setName("BP Gas Station");
            g.setType(LocationType.GAS_STATION);
            g.setActive(true);
            locationRepository.save(g);
        }

        if (bootstrapAdminPin != null && !bootstrapAdminPin.isBlank()) {
            var admins = employeeRepository.findAllByRoleAndActiveIsTrue(EmployeeRole.SUPER_ADMIN);
            if (admins.isEmpty()) {
                var admin = new com.example.timetracking.model.Employee();
                admin.setFullName("Boss");
                admin.setRole(EmployeeRole.SUPER_ADMIN);
                admin.setActive(true);
                admin.setCreatedAt(Instant.now(clock));
                admin.setPinHash(pinService.hashPin(bootstrapAdminPin));
                employeeRepository.save(admin);
                System.out.println("========================================");
                System.out.println("BOOTSTRAP: Set SUPER_ADMIN PIN to configured value");
                System.out.println("Name: Boss");
                System.out.println("ADMIN PIN: " + bootstrapAdminPin);
                System.out.println("Login at: /admin/login");
                System.out.println("========================================");
            } else {
                for (var admin : admins) {
                    admin.setPinHash(pinService.hashPin(bootstrapAdminPin));
                }
                employeeRepository.saveAll(admins);
                System.out.println("========================================");
                System.out.println("BOOTSTRAP: Reset SUPER_ADMIN PIN to configured value");
                System.out.println("ADMIN PIN: " + bootstrapAdminPin);
                System.out.println("Login at: /admin/login");
                System.out.println("========================================");
            }
        } else if (employeeRepository.findAllByRoleAndActiveIsTrue(EmployeeRole.SUPER_ADMIN).isEmpty()) {
            var created = employeeService.createEmployee("Boss", EmployeeRole.SUPER_ADMIN);
            System.out.println("========================================");
            System.out.println("Created initial SUPER_ADMIN");
            System.out.println("Name: Boss");
            System.out.println("ADMIN PIN: " + created.rawPin());
            System.out.println("Login at: /admin/login");
            System.out.println("========================================");
        }
    }
}
