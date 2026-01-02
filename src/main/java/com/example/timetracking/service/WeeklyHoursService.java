package com.example.timetracking.service;

import com.example.timetracking.model.TimeEntry;
import com.example.timetracking.repo.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeeklyHoursService {

    private final TimeEntryRepository timeEntryRepository;

    private final ZoneId zone = ZoneId.of("UTC");

    @Transactional(readOnly = true)
    public List<EmployeeWeeklyHours> computeWeeklyHours(Instant weekStartInclusive, Instant weekEndExclusive) {
        List<TimeEntry> entries = timeEntryRepository.findCompletedOverlapping(weekStartInclusive, weekEndExclusive);

        Map<Long, EmployeeWeeklyHoursBuilder> byEmployee = new HashMap<>();

        for (TimeEntry t : entries) {
            if (t.getEmployee() == null || t.getEmployee().getId() == null) {
                continue;
            }
            if (t.getClockInTime() == null || t.getClockOutTime() == null) {
                continue;
            }

            Instant start = t.getClockInTime().isAfter(weekStartInclusive) ? t.getClockInTime() : weekStartInclusive;
            Instant end = t.getClockOutTime().isBefore(weekEndExclusive) ? t.getClockOutTime() : weekEndExclusive;
            if (!end.isAfter(start)) {
                continue;
            }

            EmployeeWeeklyHoursBuilder b = byEmployee.computeIfAbsent(
                t.getEmployee().getId(),
                id -> new EmployeeWeeklyHoursBuilder(id, t.getEmployee().getFullName())
            );
            b.completedTotal = b.completedTotal.plus(Duration.between(start, end));
        }

        List<EmployeeWeeklyHours> result = new ArrayList<>();
        for (EmployeeWeeklyHoursBuilder b : byEmployee.values()) {
            result.add(new EmployeeWeeklyHours(b.employeeId, b.employeeName, b.completedTotal, formatDuration(b.completedTotal)));
        }

        result.sort(Comparator.comparing(EmployeeWeeklyHours::employeeName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    @Transactional(readOnly = true)
    public WeeklyReport computeWeeklyReport(Instant weekStartInclusive, Instant weekEndExclusive, Instant now) {
        List<TimeEntry> entries = timeEntryRepository.findOverlappingWithEmployee(weekStartInclusive, weekEndExclusive);

        Map<Long, EmployeeWeeklyHoursBuilder> byEmployee = new HashMap<>();
        List<ShiftRow> shifts = new ArrayList<>();

        for (TimeEntry t : entries) {
            if (t.getEmployee() == null || t.getEmployee().getId() == null) {
                continue;
            }
            if (t.getClockInTime() == null) {
                continue;
            }

            Instant in = t.getClockInTime();
            Instant out = t.getClockOutTime();
            Instant effectiveOut = out == null ? now : out;

            Instant clipStart = in.isAfter(weekStartInclusive) ? in : weekStartInclusive;
            Instant clipEnd = effectiveOut.isBefore(weekEndExclusive) ? effectiveOut : weekEndExclusive;
            Duration liveDuration = clipEnd.isAfter(clipStart) ? Duration.between(clipStart, clipEnd) : Duration.ZERO;

            boolean completed = out != null;

            EmployeeWeeklyHoursBuilder b = byEmployee.computeIfAbsent(
                t.getEmployee().getId(),
                id -> new EmployeeWeeklyHoursBuilder(id, t.getEmployee().getFullName())
            );
            b.liveTotal = b.liveTotal.plus(liveDuration);
            if (completed) {
                b.completedTotal = b.completedTotal.plus(liveDuration);
            }

            LocalDate day = in.atZone(zone).toLocalDate();
            shifts.add(new ShiftRow(
                t.getId() == null ? -1L : t.getId(),
                t.getEmployee().getId(),
                t.getEmployee().getFullName(),
                day.toString(),
                in.toEpochMilli(),
                out == null ? null : out.toEpochMilli(),
                liveDuration.getSeconds(),
                completed
            ));
        }

        List<EmployeeWeeklyTotals> totals = new ArrayList<>();
        for (EmployeeWeeklyHoursBuilder b : byEmployee.values()) {
            totals.add(new EmployeeWeeklyTotals(
                b.employeeId,
                b.employeeName,
                formatDuration(b.completedTotal),
                formatDuration(b.liveTotal)
            ));
        }
        totals.sort(Comparator.comparing(EmployeeWeeklyTotals::employeeName, String.CASE_INSENSITIVE_ORDER));
        shifts.sort(Comparator.comparing(ShiftRow::clockInEpochMillis).reversed().thenComparing(ShiftRow::employeeName, String.CASE_INSENSITIVE_ORDER));

        return new WeeklyReport(totals, shifts, now.toEpochMilli());
    }

    private String formatDuration(Duration d) {
        long seconds = d.getSeconds();
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        return String.format("%d:%02d", h, m);
    }

    private static class EmployeeWeeklyHoursBuilder {
        private final long employeeId;
        private final String employeeName;
        private Duration completedTotal = Duration.ZERO;
        private Duration liveTotal = Duration.ZERO;

        private EmployeeWeeklyHoursBuilder(long employeeId, String employeeName) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
        }
    }

    public record EmployeeWeeklyHours(long employeeId, String employeeName, Duration total, String totalFormatted) {}

    public record EmployeeWeeklyTotals(long employeeId, String employeeName, String completedTotalFormatted, String liveTotalFormatted) {}

    public record ShiftRow(
        long timeEntryId,
        long employeeId,
        String employeeName,
        String day,
        long clockInEpochMillis,
        Long clockOutEpochMillis,
        long workedSeconds,
        boolean completed
    ) {}

    public record WeeklyReport(List<EmployeeWeeklyTotals> totals, List<ShiftRow> shifts, long serverNowEpochMillis) {}
}
