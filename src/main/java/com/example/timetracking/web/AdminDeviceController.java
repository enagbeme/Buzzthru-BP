package com.example.timetracking.web;

import com.example.timetracking.repo.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminDeviceController {

    private final DeviceRepository deviceRepository;

    @GetMapping("/admin/devices")
    public String devices(Model model,
                          @RequestParam(value = "message", required = false) String message) {
        model.addAttribute("devices", deviceRepository.findAllWithLocation());
        model.addAttribute("message", message);
        return "admin/devices";
    }

    @PostMapping("/admin/devices/toggle-active")
    public String toggleActive(@RequestParam("deviceId") long deviceId,
                               RedirectAttributes redirectAttributes) {
        var d = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new IllegalArgumentException("Device not found"));
        d.setActive(!d.isActive());
        deviceRepository.save(d);
        redirectAttributes.addAttribute("message", "Device updated.");
        return "redirect:/admin/devices";
    }
}
