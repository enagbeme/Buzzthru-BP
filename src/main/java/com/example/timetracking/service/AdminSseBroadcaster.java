package com.example.timetracking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AdminSseBroadcaster {

    private final AdminSseService adminSseService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTimeClockEvent(TimeClockEvent event) {
        adminSseService.broadcast("open-shifts", "refresh");
        adminSseService.broadcast("reports", "refresh");
    }
}
