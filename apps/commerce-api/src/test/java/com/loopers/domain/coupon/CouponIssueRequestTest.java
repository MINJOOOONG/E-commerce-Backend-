package com.loopers.domain.coupon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CouponIssueRequestTest {

    @DisplayName("CouponIssueRequest 생성 시,")
    @Nested
    class Create {

        @DisplayName("초기 상태는 PENDING이다.")
        @Test
        void createsWithPendingStatus() {
            CouponIssueRequest request = new CouponIssueRequest(1L, 10L);

            assertThat(request.getUserId()).isEqualTo(1L);
            assertThat(request.getCouponTemplateId()).isEqualTo(10L);
            assertThat(request.getRequestId()).isNotNull();
            assertThat(request.getStatus()).isEqualTo(CouponIssueStatus.PENDING);
        }
    }

    @DisplayName("상태 전이 시,")
    @Nested
    class StatusTransition {

        @DisplayName("succeed 호출 시 SUCCESS로 변경된다.")
        @Test
        void changesStatusToSuccess() {
            CouponIssueRequest request = new CouponIssueRequest(1L, 10L);

            request.succeed();

            assertThat(request.getStatus()).isEqualTo(CouponIssueStatus.SUCCESS);
        }

        @DisplayName("fail 호출 시 FAILED로 변경되고 사유가 기록된다.")
        @Test
        void changesStatusToFailedWithReason() {
            CouponIssueRequest request = new CouponIssueRequest(1L, 10L);

            request.fail("수량 소진");

            assertThat(request.getStatus()).isEqualTo(CouponIssueStatus.FAILED);
            assertThat(request.getFailureReason()).isEqualTo("수량 소진");
        }
    }
}
