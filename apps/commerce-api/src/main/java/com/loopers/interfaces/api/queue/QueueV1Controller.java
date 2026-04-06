package com.loopers.interfaces.api.queue;

import com.loopers.application.queue.QueueFacade;
import com.loopers.application.queue.QueueInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/queue")
public class QueueV1Controller {

    private final QueueFacade queueFacade;

    @PostMapping("/enter")
    public ApiResponse<QueueV1Dto.EnterResponse> enter(
        @RequestHeader("X-Loopers-UserId") Long userId
    ) {
        QueueInfo.EnterResult info = queueFacade.enter(userId);
        return ApiResponse.success(QueueV1Dto.EnterResponse.from(info));
    }

    @GetMapping("/position")
    public ApiResponse<QueueV1Dto.PositionResponse> getPosition(
        @RequestHeader("X-Loopers-UserId") Long userId
    ) {
        QueueInfo.PositionResult info = queueFacade.getPosition(userId);
        return ApiResponse.success(QueueV1Dto.PositionResponse.from(info));
    }
}
