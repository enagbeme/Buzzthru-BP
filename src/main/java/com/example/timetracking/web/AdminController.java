package com.example.timetracking.web;

import com.example.timetracking.model.EmployeeRole;
import com.example.timetracking.model.Location;
import com.example.timetracking.model.LocationType;
import com.example.timetracking.repo.EmployeeRepository;
import com.example.timetracking.repo.LocationRepository;
import com.example.timetracking.repo.TimeEntryRepository;
import com.example.timetracking.service.DeviceCookieService;
import com.example.timetracking.service.DeviceService;
import com.example.timetracking.service.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;
    private final LocationRepository locationRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final DeviceCookieService deviceCookieService;
    private final DeviceService deviceService;
    private final Clock clock;

    @GetMapping("/admin/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping("/admin")
    public String dashboard(HttpServletRequest request, HttpServletResponse response,
                            Model model, Authentication authentication,
                            @RequestParam(value = "message", required = false) String message) {
        model.addAttribute("serverEpochMillis", Instant.now(clock).toEpochMilli());
        List<Location> locations = locationRepository.findAll();
        model.addAttribute("locations", locations);
        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("message", message);

        boolean deviceRegistered = false;
        String deviceUuid = deviceCookieService.readDeviceUuid(request).orElse(null);
        if (deviceUuid != null && !deviceUuid.isBlank()) {
            try {
                deviceService.requireActiveRegisteredDevice(deviceUuid);
                deviceRegistered = true;
            } catch (RuntimeException ignored) {
                deviceRegistered = false;
            }
        }
        model.addAttribute("deviceRegistered", deviceRegistered);

        var zone = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate weekStart = today.with(WeekFields.of(Locale.US).dayOfWeek(), 1);
        model.addAttribute("weekStart", weekStart);

        model.addAttribute("openByLocation", locations.stream().collect(java.util.stream.Collectors.toMap(
            Location::getId,
            loc -> timeEntryRepository.findOpenByLocationWithEmployee(loc)
        )));

        model.addAttribute("adminId", authentication == null ? null : authentication.getName());
        return "admin/dashboard";
    }

    @GetMapping("/admin/register-device")
    public String registerDevicePage(HttpServletRequest request, HttpServletResponse response, Model model,
                                     @RequestParam(value = "message", required = false) String message) {
        String deviceUuid = deviceCookieService.readDeviceUuid(request)
            .orElseGet(() -> deviceCookieService.ensureDeviceUuidCookie(response));

        model.addAttribute("deviceUuid", deviceUuid);
        model.addAttribute("locations", locationRepository.findAll());
        model.addAttribute("message", message);
        return "admin/register-device";
    }

    @PostMapping("/admin/register-device")
    public String registerDevice(HttpServletRequest request, HttpServletResponse response,
                                 @RequestParam("locationId") long locationId,
                                 @RequestParam(value = "computerName", required = false) String computerName,
                                 RedirectAttributes redirectAttributes) {
        String deviceUuid = deviceCookieService.readDeviceUuid(request)
            .orElseGet(() -> deviceCookieService.ensureDeviceUuidCookie(response));

        deviceService.registerDeviceToLocation(deviceUuid, locationId, computerName);
        redirectAttributes.addAttribute("message", "Device registered.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/locations/create")
    public String createLocation(@RequestParam("name") String name,
                                 @RequestParam("type") LocationType type,
                                 @RequestParam(value = "address", required = false) String address,
                                 RedirectAttributes redirectAttributes) {
        Location l = new Location();
        l.setName(name);
        l.setType(type);
        l.setAddress(address);
        l.setActive(true);
        locationRepository.save(l);
        redirectAttributes.addAttribute("message", "Location created.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/employees/create")
    public String createEmployee(@RequestParam("fullName") String fullName,
                                 @RequestParam("role") EmployeeRole role,
                                 RedirectAttributes redirectAttributes) {
        var created = employeeService.createEmployee(fullName, role);
        redirectAttributes.addAttribute("message", "Employee created. PIN: " + created.rawPin());
        return "redirect:/admin";
    }

    @PostMapping("/admin/employees/reset-pin")
    public String resetEmployeePin(@RequestParam("employeeId") long employeeId,
                                   RedirectAttributes redirectAttributes) {
        String pin = employeeService.resetPin(employeeId);
        redirectAttributes.addAttribute("message", "PIN reset. New PIN: " + pin);
        return "redirect:/admin";
    }

    @PostMapping("/admin/employees/toggle-active")
    public String toggleEmployeeActive(@RequestParam("employeeId") long employeeId,
                                       RedirectAttributes redirectAttributes) {
        var e = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        e.setActive(!e.isActive());
        employeeRepository.save(e);
        redirectAttributes.addAttribute("message", "Employee updated.");
        return "redirect:/admin";
    }
}
