package com.example.timetracking.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceCookieService {

    @Value("${app.device.cookie-name:TT_DEVICE}")
    private String cookieName;

    @Value("${app.device.cookie-max-age-seconds:31536000}")
    private int cookieMaxAgeSeconds;

    public Optional<String> readDeviceUuid(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    public String ensureDeviceUuidCookie(HttpServletResponse response) {
        String uuid = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(cookieName, uuid);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(cookieMaxAgeSeconds);
        response.addCookie(cookie);
        return uuid;
    }
}
