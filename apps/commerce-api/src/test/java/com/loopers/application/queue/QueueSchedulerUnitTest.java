package com.loopers.application.queue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueScheduler 단위 테스트")
class QueueSchedulerUnitTest {

    @Mock
    private QueueFacade queueFacade;

    @InjectMocks
    private QueueScheduler queueScheduler;

    @DisplayName("스케줄러가 실행되면 QueueFacade.processQueue를 호출한다.")
    @Test
    void scheduleQueueProcessing_callsProcessQueue() {
        // arrange
        doNothing().when(queueFacade).processQueue();

        // act
        queueScheduler.scheduleQueueProcessing();

        // assert
        verify(queueFacade).processQueue();
    }
}
