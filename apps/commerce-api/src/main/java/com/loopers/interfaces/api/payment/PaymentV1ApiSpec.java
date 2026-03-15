package com.loopers.interfaces.api.payment;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Payment V1 API", description = "결제 API")
public interface PaymentV1ApiSpec {

    @Operation(
        summary = "결제 요청",
        description = "주문에 대한 PG 결제를 요청합니다. 결제 결과는 비동기로 callback을 통해 확정됩니다."
    )
    ApiResponse<PaymentV1Dto.PaymentResponse> requestPayment(PaymentV1Dto.PaymentRequest request);

    @Operation(
        summary = "PG 결제 callback",
        description = "PG사로부터 결제 결과 callback을 수신합니다."
    )
    ApiResponse<PaymentV1Dto.PaymentResponse> handleCallback(PaymentV1Dto.CallbackRequest request);
}
