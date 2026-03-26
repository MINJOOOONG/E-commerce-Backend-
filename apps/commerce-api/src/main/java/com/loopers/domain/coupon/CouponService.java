package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class CouponService {

    private final CouponTemplateRepository couponTemplateRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponIssueRequestRepository couponIssueRequestRepository;

    @Transactional
    public void issueWithLimit(String requestId, Long userId, Long couponTemplateId) {
        CouponIssueRequest request = couponIssueRequestRepository.findByRequestId(requestId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "발급 요청을 찾을 수 없습니다"));

        try {
            if (userCouponRepository.existsByUserIdAndCouponTemplateId(userId, couponTemplateId)) {
                request.fail("이미 발급받은 쿠폰입니다 (중복 발급 불가)");
                return;
            }

            CouponTemplate template = couponTemplateRepository.findByIdWithLock(couponTemplateId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 템플릿을 찾을 수 없습니다"));

            template.issue();
            userCouponRepository.save(new UserCoupon(userId, couponTemplateId));
            request.succeed();

            log.info("[CouponService] 쿠폰 발급 성공 - requestId={}, userId={}, templateId={}",
                    requestId, userId, couponTemplateId);
        } catch (CoreException e) {
            if (e.getErrorType() == ErrorType.CONFLICT) {
                request.fail("쿠폰이 모두 소진되었습니다");
                log.info("[CouponService] 쿠폰 소진 - requestId={}", requestId);
            } else {
                throw e;
            }
        }
    }
}
