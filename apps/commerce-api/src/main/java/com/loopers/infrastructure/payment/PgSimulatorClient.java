package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PgClient;
import com.loopers.domain.payment.PgPaymentRequest;
import com.loopers.domain.payment.PgPaymentResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class PgSimulatorClient implements PgClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PgSimulatorClient(
        @Qualifier("pgRestTemplate") RestTemplate restTemplate,
        @Value("${pg.simulator.base-url:http://localhost:8081}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    @CircuitBreaker(name = "pgClient")
    @Bulkhead(name = "pgClient")
    public PgPaymentResponse requestPayment(PgPaymentRequest request) {
        return restTemplate.postForObject(
            baseUrl + "/api/v1/payments",
            request,
            PgPaymentResponse.class
        );
    }

    @Override
    @CircuitBreaker(name = "pgClient")
    @Bulkhead(name = "pgClient")
    public PgPaymentResponse queryPaymentStatus(String pgTransactionId) {
        return restTemplate.getForObject(
            baseUrl + "/api/v1/payments/{pgTransactionId}",
            PgPaymentResponse.class,
            pgTransactionId
        );
    }

    @Override
    @CircuitBreaker(name = "pgClient")
    @Bulkhead(name = "pgClient")
    public PgPaymentResponse cancelPayment(String pgTransactionId) {
        return restTemplate.postForObject(
            baseUrl + "/api/v1/payments/{pgTransactionId}/cancel",
            null,
            PgPaymentResponse.class,
            pgTransactionId
        );
    }
}
