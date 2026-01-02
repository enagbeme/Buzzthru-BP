package com.example.timetracking.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PinAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final int LOCK_MINUTES = 5;

    private final Map<String, State> states = new ConcurrentHashMap<>();
    private final Clock clock;

    public PinAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        State s = states.get(key);
        if (s == null) {
            return;
        }
        if (s.lockedUntil != null && Instant.now(clock).isBefore(s.lockedUntil)) {
            throw new IllegalStateException("Too many failed attempts. Try again later.");
        }
    }

    public void recordSuccess(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        states.remove(key);
    }

    public void recordFailure(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        State s = states.computeIfAbsent(key, k -> new State());
        Instant now = Instant.now(clock);

        if (s.lockedUntil != null && now.isAfter(s.lockedUntil)) {
            s.failures = 0;
            s.lockedUntil = null;
        }

        s.failures++;
        if (s.failures >= MAX_FAILURES) {
            s.lockedUntil = now.plusSeconds(LOCK_MINUTES * 60L);
        }
    }

    private static class State {
        private int failures = 0;
        private Instant lockedUntil;
    }
}
