package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponTemplateRepository couponTemplateRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private CouponIssueRequestRepository couponIssueRequestRepository;

    @InjectMocks
    private CouponService couponService;

    @DisplayName("issueWithLimit 호출 시,")
    @Nested
    class IssueWithLimit {

        @DisplayName("정상 발급되면 request 상태가 SUCCESS가 된다.")
        @Test
        void issuesSuccessfully() {
            // arrange
            CouponTemplate template = new CouponTemplate("쿠폰", 10);
            CouponIssueRequest request = new CouponIssueRequest(1L, 1L);

            when(couponIssueRequestRepository.findByRequestId(request.getRequestId()))
                .thenReturn(Optional.of(request));
            when(couponTemplateRepository.findByIdWithLock(1L))
                .thenReturn(Optional.of(template));
            when(userCouponRepository.existsByUserIdAndCouponTemplateId(1L, 1L))
                .thenReturn(false);
            when(userCouponRepository.save(any(UserCoupon.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // act
            couponService.issueWithLimit(request.getRequestId(), 1L, 1L);

            // assert
            assertThat(request.getStatus()).isEqualTo(CouponIssueStatus.SUCCESS);
            assertThat(template.getIssuedCount()).isEqualTo(1);
            verify(userCouponRepository).save(any(UserCoupon.class));
        }

        @DisplayName("이미 발급받은 유저면 request 상태가 FAILED가 된다.")
        @Test
        void failsWhenDuplicate() {
            // arrange
            CouponIssueRequest request = new CouponIssueRequest(1L, 1L);

            when(couponIssueRequestRepository.findByRequestId(request.getRequestId()))
                .thenReturn(Optional.of(request));
            when(userCouponRepository.existsByUserIdAndCouponTemplateId(1L, 1L))
                .thenReturn(true);

            // act
            couponService.issueWithLimit(request.getRequestId(), 1L, 1L);

            // assert
            assertThat(request.getStatus()).isEqualTo(CouponIssueStatus.FAILED);
            assertThat(request.getFailureReason()).contains("중복");
            verify(userCouponRepository, never()).save(any());
        }

        @DisplayName("수량이 소진되면 request 상태가 FAILED가 된다.")
        @Test
        void failsWhenSoldOut() {
            // arrange
            CouponTemplate template = new CouponTemplate("쿠폰", 1);
            template.issue(); // 1개 소진
            CouponIssueRequest request = new CouponIssueRequest(2L, 1L);

            when(couponIssueRequestRepository.findByRequestId(request.getRequestId()))
                .thenReturn(Optional.of(request));
            when(couponTemplateRepository.findByIdWithLock(1L))
                .thenReturn(Optional.of(template));
            when(userCouponRepository.existsByUserIdAndCouponTemplateId(2L, 1L))
                .thenReturn(false);

            // act
            couponService.issueWithLimit(request.getRequestId(), 2L, 1L);

            // assert
            assertThat(request.getStatus()).isEqualTo(CouponIssueStatus.FAILED);
            assertThat(request.getFailureReason()).contains("소진");
            verify(userCouponRepository, never()).save(any());
        }
    }
}
