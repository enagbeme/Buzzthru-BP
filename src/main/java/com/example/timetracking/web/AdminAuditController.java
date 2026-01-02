package com.example.timetracking.web;

import com.example.timetracking.repo.TimeEntryAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminAuditController {

    private final TimeEntryAuditRepository timeEntryAuditRepository;

    @GetMapping("/admin/audit")
    public String audit(Model model) {
        model.addAttribute("audits", timeEntryAuditRepository.findLatestWithDetails());
        return "admin/audit";
    }
}
