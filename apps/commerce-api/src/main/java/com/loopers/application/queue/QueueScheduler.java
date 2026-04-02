package com.loopers.application.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class QueueScheduler {

    private final QueueFacade queueFacade;

    @Scheduled(fixedDelay = 1000)
    public void scheduleQueueProcessing() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
