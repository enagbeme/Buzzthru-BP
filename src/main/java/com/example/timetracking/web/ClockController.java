package com.example.timetracking.web;

import com.example.timetracking.model.Device;
import com.example.timetracking.repo.TimeEntryRepository;
import com.example.timetracking.service.DeviceCookieService;
import com.example.timetracking.service.DeviceService;
import com.example.timetracking.service.TimeClockService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class ClockController {

    private final DeviceCookieService deviceCookieService;
    private final DeviceService deviceService;
    private final TimeClockService timeClockService;
    private final TimeEntryRepository timeEntryRepository;
    private final Clock clock;

    @GetMapping("/clock")
    public String clockHome(HttpServletRequest request, HttpServletResponse response, Model model,
                            @RequestParam(value = "clockInMessage", required = false) String clockInMessage,
                            @RequestParam(value = "clockOutMessage", required = false) String clockOutMessage) {
        String deviceUuid = deviceCookieService.readDeviceUuid(request)
            .orElseGet(() -> deviceCookieService.ensureDeviceUuidCookie(response));

        model.addAttribute("serverEpochMillis", Instant.now(clock).toEpochMilli());

        try {
            Device device = deviceService.requireActiveRegisteredDevice(deviceUuid);
            model.addAttribute("locationName", device.getLocation().getName());

            var openEntries = timeEntryRepository.findOpenByLocationWithEmployee(device.getLocation());
            model.addAttribute("openEntries", openEntries);

            model.addAttribute("status", openEntries.isEmpty() ? "NOT_CLOCKED_IN" : "CLOCKED_IN");
            if (!openEntries.isEmpty()) {
                var entry = openEntries.get(0);
                model.addAttribute("clockedInEmployeeName", entry.getEmployee().getFullName());
                model.addAttribute("clockInTime", entry.getClockInTime());
                Duration running = Duration.between(entry.getClockInTime(), Instant.now(clock));
                model.addAttribute("runningTime", formatDuration(running));
            }

            model.addAttribute("clockInMessage", clockInMessage);
            model.addAttribute("clockOutMessage", clockOutMessage);
            return "clock";
        } catch (RuntimeException ex) {
            model.addAttribute("locationName", "Unregistered Device");
            model.addAttribute("openEntries", java.util.List.of());
            model.addAttribute("status", "NOT_CLOCKED_IN");
            model.addAttribute("clockInMessage", ex.getMessage());
            return "clock";
        }
    }

    @PostMapping("/clock/clock-in")
    public String clockIn(HttpServletRequest request, HttpServletResponse response,
                          @RequestParam("pin") String pin,
                          RedirectAttributes redirectAttributes) {
        String deviceUuid = deviceCookieService.readDeviceUuid(request)
            .orElseGet(() -> deviceCookieService.ensureDeviceUuidCookie(response));

        Device device = deviceService.requireActiveRegisteredDevice(deviceUuid);
        try {
            var result = timeClockService.clockIn(device, pin);
            redirectAttributes.addAttribute("clockInMessage", "Clock-in successful: " + result.employeeName() +
                " | In: " + formatInstant(result.clockInTime()));
        } catch (RuntimeException ex) {
            redirectAttributes.addAttribute("clockInMessage", ex.getMessage());
        }
        return "redirect:/clock";
    }

    @PostMapping("/clock/clock-out")
    public String clockOut(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam("pin") String pin,
                           RedirectAttributes redirectAttributes) {
        String deviceUuid = deviceCookieService.readDeviceUuid(request)
            .orElseGet(() -> deviceCookieService.ensureDeviceUuidCookie(response));

        Device device = deviceService.requireActiveRegisteredDevice(deviceUuid);
        try {
            var result = timeClockService.clockOut(device, pin);
            Duration shift = Duration.between(result.clockInTime(), result.clockOutTime());
            redirectAttributes.addAttribute("clockOutMessage", "Clock-out successful: " + result.employeeName() +
                " | In: " + formatInstant(result.clockInTime()) +
                " | Out: " + formatInstant(result.clockOutTime()) +
                " | Worked: " + formatDuration(shift));
        } catch (RuntimeException ex) {
            redirectAttributes.addAttribute("clockOutMessage", ex.getMessage());
        }
        return "redirect:/clock";
    }

    @GetMapping("/clock/status")
    public String clockStatus(HttpServletRequest request, HttpServletResponse response, Model model) {
        String deviceUuid = deviceCookieService.readDeviceUuid(request)
            .orElseGet(() -> deviceCookieService.ensureDeviceUuidCookie(response));

        model.addAttribute("serverEpochMillis", Instant.now(clock).toEpochMilli());

        try {
            Device device = deviceService.requireActiveRegisteredDevice(deviceUuid);
            model.addAttribute("locationName", device.getLocation().getName());

            var openEntries = timeEntryRepository.findOpenByLocationWithEmployee(device.getLocation());
            model.addAttribute("openEntries", openEntries);
            var zone = ZoneId.of("UTC");
            LocalDate today = LocalDate.now(zone);
            Instant start = today.atStartOfDay(zone).toInstant();
            Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();

            model.addAttribute("status", "NOT_CLOCKED_IN");

            if (!openEntries.isEmpty()) {
                var entry = openEntries.get(0);
                model.addAttribute("status", "CLOCKED_IN");
                model.addAttribute("clockInTime", entry.getClockInTime());

                Duration running = Duration.between(entry.getClockInTime(), Instant.now(clock));
                model.addAttribute("runningTime", formatDuration(running));

                var todays = timeEntryRepository.findByEmployeeBetween(entry.getEmployee(), start, end);
                Duration total = Duration.ZERO;
                for (var t : todays) {
                    Instant out = t.getClockOutTime() == null ? Instant.now(clock) : t.getClockOutTime();
                    total = total.plus(Duration.between(t.getClockInTime(), out));
                }
                model.addAttribute("hoursToday", formatDuration(total));
            }

            return "clock";
        } catch (RuntimeException ex) {
            model.addAttribute("locationName", "Unregistered Device");
            model.addAttribute("status", "NOT_CLOCKED_IN");
            model.addAttribute("message", ex.getMessage());
            return "clock";
        }
    }

    private String formatDuration(Duration d) {
        long seconds = d.getSeconds();
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return "-";
        }
        var zone = ZoneId.of("UTC");
        var dt = instant.atZone(zone);
        var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);
        return dt.format(fmt) + " UTC";
    }
}
