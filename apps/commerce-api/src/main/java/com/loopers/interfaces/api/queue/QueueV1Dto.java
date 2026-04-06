package com.loopers.interfaces.api.queue;

import com.loopers.application.queue.QueueInfo;

public class QueueV1Dto {

    public record EnterResponse(Long userId, long position, long totalWaiting) {
        public static EnterResponse from(QueueInfo.EnterResult info) {
            return new EnterResponse(info.userId(), info.position(), info.totalWaiting());
        }
    }

    public record PositionResponse(Long userId, long position, long totalWaiting,
                                   long estimatedWaitSeconds, String token) {
        public static PositionResponse from(QueueInfo.PositionResult info) {
            return new PositionResponse(
                info.userId(), info.position(), info.totalWaiting(),
                info.estimatedWaitSeconds(), info.token()
            );
        }
    }
}
