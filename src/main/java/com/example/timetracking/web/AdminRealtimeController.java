package com.example.timetracking.web;

import com.example.timetracking.model.Location;
import com.example.timetracking.repo.LocationRepository;
import com.example.timetracking.repo.TimeEntryRepository;
import com.example.timetracking.service.AdminSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AdminRealtimeController {

    private final AdminSseService adminSseService;
    private final LocationRepository locationRepository;
    private final TimeEntryRepository timeEntryRepository;

    @GetMapping(path = "/admin/open-shifts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter openShiftsStream() {
        return adminSseService.subscribe();
    }

    @GetMapping("/admin/open-shifts/data")
    @ResponseBody
    public OpenShiftsSnapshot openShiftsData() {
        List<Location> locations = locationRepository.findAll();

        Map<Long, List<OpenShiftRow>> openByLocation = locations.stream().collect(Collectors.toMap(
            Location::getId,
            loc -> timeEntryRepository.findOpenByLocationWithEmployee(loc).stream().map(t ->
                new OpenShiftRow(
                    t.getEmployee() == null ? "" : t.getEmployee().getFullName(),
                    t.getClockInTime() == null ? null : t.getClockInTime().toEpochMilli(),
                    t.getClockInTime() == null ? null : t.getClockInTime().toString()
                )
            ).toList()
        ));

        List<LocationRow> locRows = locations.stream().map(l -> new LocationRow(l.getId(), l.getName())).toList();
        return new OpenShiftsSnapshot(locRows, openByLocation);
    }

    public record LocationRow(Long id, String name) {}

    public record OpenShiftRow(String employeeName, Long clockInEpochMillis, String clockInIso) {}

    public record OpenShiftsSnapshot(List<LocationRow> locations, Map<Long, List<OpenShiftRow>> openByLocation) {}
}
