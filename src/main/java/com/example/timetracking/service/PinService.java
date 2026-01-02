package com.example.timetracking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class PinService {

    private final PasswordEncoder passwordEncoder;

    private final SecureRandom random = new SecureRandom();

    @Value("${app.pin.length-min:4}")
    private int minLen;

    @Value("${app.pin.length-max:4}")
    private int maxLen;

    public String generatePin() {
        int len = minLen == maxLen ? minLen : (minLen + random.nextInt((maxLen - minLen) + 1));
        int bound = (int) Math.pow(10, len);
        int value = random.nextInt(bound);
        return String.format("%0" + len + "d", value);
    }

    public String hashPin(String pin) {
        return passwordEncoder.encode(pin);
    }

    public boolean matches(String pin, String pinHash) {
        return passwordEncoder.matches(pin, pinHash);
    }
}
