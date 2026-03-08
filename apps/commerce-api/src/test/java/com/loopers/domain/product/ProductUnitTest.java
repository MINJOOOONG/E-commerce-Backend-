package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductUnitTest {

    @DisplayName("재고를 차감할 때,")
    @Nested
    class DecreaseStock {

        @DisplayName("요청 수량이 재고보다 적으면, 재고가 차감된다.")
        @Test
        void decreasesStock_whenQuantityIsLessThanStock() {
            // arrange
            Product product = new Product(1L, "상품A", 10000L, "설명", 10);

            // act
            product.decreaseStock(3);

            // assert
            assertThat(product.getStockQuantity().value()).isEqualTo(7);
        }

        @DisplayName("요청 수량이 재고와 같으면, 재고가 0이 된다.")
        @Test
        void decreasesStockToZero_whenQuantityEqualsStock() {
            // arrange
            Product product = new Product(1L, "상품A", 10000L, "설명", 5);

            // act
            product.decreaseStock(5);

            // assert
            assertThat(product.getStockQuantity().value()).isEqualTo(0);
        }

        @DisplayName("요청 수량이 재고보다 많으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenQuantityExceedsStock() {
            // arrange
            Product product = new Product(1L, "상품A", 10000L, "설명", 3);

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                product.decreaseStock(5));

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("요청 수량이 1 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenQuantityIsLessThanOne() {
            // arrange
            Product product = new Product(1L, "상품A", 10000L, "설명", 10);

            // act & assert
            CoreException zeroResult = assertThrows(CoreException.class, () ->
                product.decreaseStock(0));
            assertThat(zeroResult.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

            CoreException negativeResult = assertThrows(CoreException.class, () ->
                product.decreaseStock(-1));
            assertThat(negativeResult.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("재고를 복원할 때,")
    @Nested
    class IncreaseStock {

        @DisplayName("요청 수량이 1 이상이면, 재고가 증가한다.")
        @Test
        void increasesStock_whenQuantityIsValid() {
            // arrange
            Product product = new Product(1L, "상품A", 10000L, "설명", 5);

            // act
            product.increaseStock(3);

            // assert
            assertThat(product.getStockQuantity().value()).isEqualTo(8);
        }
    }

    @DisplayName("좋아요 수를 증가할 때,")
    @Nested
    class IncreaseLikeCount {

        @DisplayName("increaseLikeCount를 호출하면, 좋아요 수가 1 증가한다.")
        @Test
        void increasesLikeCount_byOne() {
            // arrange
            Product product = new Product(1L, "상품A", 10000L, "설명", 10);

            // act
            product.increaseLikeCount();

            // assert
            assertThat(product.getLikeCount()).isEqualTo(1);
        }
    }

    @DisplayName("좋아요 수를 감소할 때,")
    @Nested
    class DecreaseLikeCount {

        @DisplayName("좋아요 수가 1 이상이면, 1 감소한다.")
        @Test
        void decreasesLikeCount_byOne() {
            // arrange
            Product product = new Product(1L, "상품A", 10000L, "설명", 10);
            product.increaseLikeCount();

            // act
            product.decreaseLikeCount();

            // assert
            assertThat(product.getLikeCount()).isEqualTo(0);
        }

        @DisplayName("좋아요 수가 0일 때 감소하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenLikeCountIsZero() {
            // arrange
            Product product = new Product(1L, "상품A", 10000L, "설명", 10);

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                product.decreaseLikeCount());

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
