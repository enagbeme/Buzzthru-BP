package com.example.timetracking.web;

import com.example.timetracking.model.Employee;
import com.example.timetracking.model.TimeEntry;
import com.example.timetracking.model.TimeEntryAudit;
import com.example.timetracking.repo.EmployeeRepository;
import com.example.timetracking.repo.TimeEntryAuditRepository;
import com.example.timetracking.repo.TimeEntryRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
public class AdminTimeEntryController {

    private final TimeEntryRepository timeEntryRepository;
    private final EmployeeRepository employeeRepository;
    private final TimeEntryAuditRepository timeEntryAuditRepository;

    private final ZoneId zone = ZoneId.of("UTC");

    @GetMapping("/admin/time-entries")
    public String list(Model model,
                       @RequestParam(value = "fromDate", required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                       LocalDate fromDate,
                       @RequestParam(value = "toDate", required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                       LocalDate toDate,
                       @RequestParam(value = "message", required = false) String message) {

        LocalDate today = LocalDate.now(zone);
        if (toDate == null) {
            toDate = today;
        }
        if (fromDate == null) {
            fromDate = toDate.minusDays(6);
        }
        if (toDate.isBefore(fromDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        Instant start = fromDate.atStartOfDay(zone).toInstant();
        Instant endExclusive = toDate.plusDays(1).atStartOfDay(zone).toInstant();

        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("entries", timeEntryRepository.findBetweenWithDetails(start, endExclusive));
        model.addAttribute("message", message);
        return "admin/time-entries";
    }

    @GetMapping("/admin/time-entries/edit")
    public String edit(Model model,
                       @RequestParam("id") long id,
                       @RequestParam(value = "message", required = false) String message) {
        TimeEntry entry = timeEntryRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Time entry not found"));

        model.addAttribute("entry", entry);
        model.addAttribute("clockInLocal", toDateTimeLocal(entry.getClockInTime()));
        model.addAttribute("clockOutLocal", toDateTimeLocal(entry.getClockOutTime()));
        model.addAttribute("message", message);
        return "admin/time-entry-edit";
    }

    @PostMapping("/admin/time-entries/edit")
    public String saveEdit(Authentication authentication,
                           HttpServletRequest request,
                           @RequestParam("id") long id,
                           @RequestParam("clockIn")
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                           LocalDateTime clockIn,
                           @RequestParam(value = "clockOut", required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                           LocalDateTime clockOut,
                           @RequestParam("reason") String reason,
                           RedirectAttributes redirectAttributes) {

        if (reason == null || reason.isBlank()) {
            redirectAttributes.addAttribute("message", "Reason is required.");
            redirectAttributes.addAttribute("id", id);
            return "redirect:/admin/time-entries/edit";
        }

        TimeEntry entry = timeEntryRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Time entry not found"));

        Instant oldIn = entry.getClockInTime();
        Instant oldOut = entry.getClockOutTime();
        String oldReason = entry.getEditReason();

        Instant in = clockIn.atZone(zone).toInstant();
        Instant out = (clockOut == null) ? null : clockOut.atZone(zone).toInstant();

        if (out != null && !out.isAfter(in)) {
            redirectAttributes.addAttribute("message", "Clock-out must be after clock-in.");
            redirectAttributes.addAttribute("id", id);
            return "redirect:/admin/time-entries/edit";
        }

        // Data rule: prevent multiple open shifts for same employee
        if (out == null) {
            timeEntryRepository.findFirstByEmployeeAndClockOutTimeIsNull(entry.getEmployee())
                .ifPresent(open -> {
                    if (!open.getId().equals(entry.getId())) {
                        throw new IllegalStateException("Employee already has an open shift.");
                    }
                });
        }

        entry.setClockInTime(in);
        entry.setClockOutTime(out);
        entry.setEdited(true);
        entry.setEditReason(reason);

        Employee editor = resolveAdminEmployee(authentication);
        if (editor != null) {
            entry.setEditedBy(editor);
        }

        timeEntryRepository.save(entry);

        if (editor != null) {
            if (!safeEquals(oldIn, in)) {
                timeEntryAuditRepository.save(audit(entry, editor, "clockInTime", str(oldIn), str(in)));
            }
            if (!safeEquals(oldOut, out)) {
                timeEntryAuditRepository.save(audit(entry, editor, "clockOutTime", str(oldOut), str(out)));
            }
            if (!safeEquals(oldReason, reason)) {
                timeEntryAuditRepository.save(audit(entry, editor, "editReason", oldReason, reason));
            }
        }

        redirectAttributes.addAttribute("message", "Time entry updated.");
        return "redirect:/admin/time-entries";
    }

    private TimeEntryAudit audit(TimeEntry entry, Employee editor, String field, String oldVal, String newVal) {
        TimeEntryAudit a = new TimeEntryAudit();
        a.setTimeEntry(entry);
        a.setEditedBy(editor);
        a.setFieldName(field);
        a.setOldValue(oldVal);
        a.setNewValue(newVal);
        return a;
    }

    private boolean safeEquals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private String str(Instant i) {
        return i == null ? null : i.toString();
    }

    private String toDateTimeLocal(Instant instant) {
        if (instant == null) {
            return "";
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        return instant.atZone(zone).toLocalDateTime().format(fmt);
    }

    private Employee resolveAdminEmployee(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        try {
            long employeeId = Long.parseLong(authentication.getName());
            return employeeRepository.findById(employeeId).orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
