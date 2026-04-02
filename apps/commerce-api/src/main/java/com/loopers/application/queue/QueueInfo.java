package com.loopers.application.queue;

public class QueueInfo {

    public record EnterResult(Long userId, long position, long totalWaiting) {}

    public record PositionResult(Long userId, long position, long totalWaiting,
                                 long estimatedWaitSeconds, String token) {}
}
