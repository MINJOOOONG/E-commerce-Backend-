package com.loopers.application.coupon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.coupon.CouponIssueRequest;
import com.loopers.domain.coupon.CouponIssueRequestRepository;
import com.loopers.domain.outbox.EventType;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class CouponFacade {

    private final CouponIssueRequestRepository couponIssueRequestRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public String requestIssue(Long userId, Long couponTemplateId) {
        CouponIssueRequest request = new CouponIssueRequest(userId, couponTemplateId);
        couponIssueRequestRepository.save(request);

        String payload = serializePayload(request);
        OutboxEvent outboxEvent = new OutboxEvent(EventType.COUPON_ISSUE_REQUESTED, payload);
        outboxEventRepository.save(outboxEvent);

        log.info("[CouponFacade] 쿠폰 발급 요청 저장 - requestId={}, userId={}, templateId={}",
                request.getRequestId(), userId, couponTemplateId);

        return request.getRequestId();
    }

    @Transactional(readOnly = true)
    public CouponIssueRequestInfo getIssueRequestStatus(String requestId) {
        CouponIssueRequest request = couponIssueRequestRepository.findByRequestId(requestId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "발급 요청을 찾을 수 없습니다"));
        return CouponIssueRequestInfo.from(request);
    }

    private String serializePayload(CouponIssueRequest request) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "requestId", request.getRequestId(),
                "userId", request.getUserId(),
                "couponTemplateId", request.getCouponTemplateId()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("페이로드 직렬화 실패", e);
        }
    }
}
