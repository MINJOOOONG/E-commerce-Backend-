package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.application.coupon.CouponIssueRequestInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class CouponV1Controller {

    private final CouponFacade couponFacade;

    @PostMapping("/coupons/{couponTemplateId}/issue")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<CouponV1Dto.IssueResponse> issueCoupon(
        @RequestHeader("X-Loopers-UserId") Long userId,
        @PathVariable Long couponTemplateId
    ) {
        String requestId = couponFacade.requestIssue(userId, couponTemplateId);
        return ApiResponse.success(new CouponV1Dto.IssueResponse(requestId));
    }

    @GetMapping("/coupon-requests/{requestId}")
    public ApiResponse<CouponV1Dto.IssueRequestStatusResponse> getIssueRequestStatus(
        @PathVariable String requestId
    ) {
        CouponIssueRequestInfo info = couponFacade.getIssueRequestStatus(requestId);
        return ApiResponse.success(CouponV1Dto.IssueRequestStatusResponse.from(info));
    }
}
