package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTemplateTest {

    @DisplayName("CouponTemplate 생성 시,")
    @Nested
    class Create {

        @DisplayName("이름과 수량을 전달하면 정상 생성된다.")
        @Test
        void createsSuccessfully() {
            CouponTemplate template = new CouponTemplate("선착순 쿠폰", 100);

            assertThat(template.getName()).isEqualTo("선착순 쿠폰");
            assertThat(template.getTotalQuantity()).isEqualTo(100);
            assertThat(template.getIssuedCount()).isEqualTo(0);
        }

        @DisplayName("총 수량이 0 이하이면 예외가 발생한다.")
        @Test
        void throwsException_whenTotalQuantityIsZeroOrLess() {
            assertThatThrownBy(() -> new CouponTemplate("쿠폰", 0))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("issue 호출 시,")
    @Nested
    class Issue {

        @DisplayName("수량이 남아있으면 issuedCount가 1 증가한다.")
        @Test
        void incrementsIssuedCount() {
            CouponTemplate template = new CouponTemplate("쿠폰", 10);

            template.issue();

            assertThat(template.getIssuedCount()).isEqualTo(1);
        }

        @DisplayName("수량이 모두 소진되면 CONFLICT 예외가 발생한다.")
        @Test
        void throwsException_whenSoldOut() {
            CouponTemplate template = new CouponTemplate("쿠폰", 1);
            template.issue();

            assertThatThrownBy(template::issue)
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.CONFLICT);
        }
    }
}
