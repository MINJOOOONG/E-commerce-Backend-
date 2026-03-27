package com.loopers.domain.coupon;

import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository;
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository;
import com.loopers.infrastructure.coupon.UserCouponJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponIssueConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponTemplateJpaRepository couponTemplateJpaRepository;

    @Autowired
    private CouponIssueRequestJpaRepository couponIssueRequestJpaRepository;

    @Autowired
    private UserCouponJpaRepository userCouponJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("쿠폰 수량 100개에 1000명이 동시 요청하면, 정확히 100개만 발급된다.")
    @Test
    void only100CouponsIssuedWhen1000ConcurrentRequests() throws InterruptedException {
        // arrange
        int totalQuantity = 100;
        int totalRequests = 1000;

        CouponTemplate template = couponTemplateJpaRepository.save(
            new CouponTemplate("선착순 쿠폰", totalQuantity)
        );

        List<CouponIssueRequest> requests = new ArrayList<>();
        for (int i = 1; i <= totalRequests; i++) {
            requests.add(couponIssueRequestJpaRepository.save(
                new CouponIssueRequest((long) i, template.getId())
            ));
        }

        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        // act
        for (CouponIssueRequest request : requests) {
            executor.submit(() -> {
                try {
                    couponService.issueWithLimit(
                        request.getRequestId(),
                        request.getUserId(),
                        template.getId()
                    );
                } catch (Exception e) {
                    // 예외 발생해도 latch는 감소
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        // assert
        CouponTemplate updated = couponTemplateJpaRepository.findById(template.getId()).orElseThrow();
        long successCount = couponIssueRequestJpaRepository.findAll().stream()
            .filter(r -> r.getStatus() == CouponIssueStatus.SUCCESS)
            .count();
        long failedCount = couponIssueRequestJpaRepository.findAll().stream()
            .filter(r -> r.getStatus() == CouponIssueStatus.FAILED)
            .count();
        long userCouponCount = userCouponJpaRepository.count();

        // 1. issuedCount == 100
        assertThat(updated.getIssuedCount()).isEqualTo(totalQuantity);
        // 2. 성공 발급 == 100
        assertThat(successCount).isEqualTo(totalQuantity);
        // 3. 실패 존재 (900건)
        assertThat(failedCount).isEqualTo(totalRequests - totalQuantity);
        // 4. 실제 UserCoupon == 100
        assertThat(userCouponCount).isEqualTo(totalQuantity);
        // 5. 중복 유저 없음 (각 userId가 고유)
        long distinctUserCount = userCouponJpaRepository.findAll().stream()
            .map(UserCoupon::getUserId)
            .distinct()
            .count();
        assertThat(distinctUserCount).isEqualTo(userCouponCount);
    }

    @DisplayName("같은 유저가 2번 요청하면, 1번만 성공한다.")
    @Test
    void rejectsDuplicateUserIssue() throws InterruptedException {
        // arrange
        CouponTemplate template = couponTemplateJpaRepository.save(
            new CouponTemplate("중복 테스트 쿠폰", 10)
        );

        Long sameUserId = 999L;
        CouponIssueRequest request1 = couponIssueRequestJpaRepository.save(
            new CouponIssueRequest(sameUserId, template.getId())
        );
        CouponIssueRequest request2 = couponIssueRequestJpaRepository.save(
            new CouponIssueRequest(sameUserId, template.getId())
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // act
        for (CouponIssueRequest request : List.of(request1, request2)) {
            executor.submit(() -> {
                try {
                    couponService.issueWithLimit(
                        request.getRequestId(),
                        sameUserId,
                        template.getId()
                    );
                } catch (Exception e) {
                    // ignore
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // assert
        long successCount = couponIssueRequestJpaRepository.findAll().stream()
            .filter(r -> r.getUserId().equals(sameUserId))
            .filter(r -> r.getStatus() == CouponIssueStatus.SUCCESS)
            .count();
        long userCouponCount = userCouponJpaRepository.findAll().stream()
            .filter(uc -> uc.getUserId().equals(sameUserId))
            .count();

        assertThat(successCount).isEqualTo(1);
        assertThat(userCouponCount).isEqualTo(1);
    }
}
