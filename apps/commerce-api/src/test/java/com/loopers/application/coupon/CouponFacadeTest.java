package com.loopers.application.coupon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.coupon.CouponIssueRequest;
import com.loopers.domain.coupon.CouponIssueRequestRepository;
import com.loopers.domain.coupon.CouponIssueStatus;
import com.loopers.domain.outbox.EventType;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponFacadeTest {

    @Mock
    private CouponIssueRequestRepository couponIssueRequestRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CouponFacade couponFacade;

    @DisplayName("requestIssue 호출 시,")
    @Nested
    class RequestIssue {

        @DisplayName("CouponIssueRequest를 PENDING으로 저장하고 OutboxEvent를 저장한다.")
        @Test
        void savesRequestAndOutbox() {
            // arrange
            when(couponIssueRequestRepository.save(any(CouponIssueRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // act
            String requestId = couponFacade.requestIssue(1L, 10L);

            // assert
            assertThat(requestId).isNotNull();

            ArgumentCaptor<CouponIssueRequest> reqCaptor = ArgumentCaptor.forClass(CouponIssueRequest.class);
            verify(couponIssueRequestRepository).save(reqCaptor.capture());
            assertThat(reqCaptor.getValue().getStatus()).isEqualTo(CouponIssueStatus.PENDING);

            ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(outboxCaptor.capture());
            assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.COUPON_ISSUE_REQUESTED);
            assertThat(outboxCaptor.getValue().getPayload()).contains("\"userId\":1");
        }
    }

    @DisplayName("getIssueRequestStatus 호출 시,")
    @Nested
    class GetIssueRequestStatus {

        @DisplayName("requestId로 조회하면 상태 정보를 반환한다.")
        @Test
        void returnsRequestInfo() {
            // arrange
            CouponIssueRequest request = new CouponIssueRequest(1L, 10L);
            when(couponIssueRequestRepository.findByRequestId(request.getRequestId()))
                .thenReturn(Optional.of(request));

            // act
            CouponIssueRequestInfo info = couponFacade.getIssueRequestStatus(request.getRequestId());

            // assert
            assertThat(info.requestId()).isEqualTo(request.getRequestId());
            assertThat(info.status()).isEqualTo(CouponIssueStatus.PENDING);
        }
    }
}
