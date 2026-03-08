package com.loopers.interfaces.api.product;

import com.loopers.domain.product.Product;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    private Product saveProduct(Long brandId, String name, Long price, String description, Integer stock) {
        return productJpaRepository.save(new Product(brandId, name, price, description, stock));
    }

    private Product saveProductWithLikes(Long brandId, String name, Long price, String description, Integer stock, int likeCount) {
        Product product = new Product(brandId, name, price, description, stock);
        for (int i = 0; i < likeCount; i++) {
            product.increaseLikeCount();
        }
        return productJpaRepository.save(product);
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    class GetProduct {

        @DisplayName("존재하는 상품을 조회하면, 상품 정보와 likeCount를 반환한다.")
        @Test
        void returnsProduct_whenProductExists() {
            // arrange
            Product saved = saveProductWithLikes(1L, "테스트 상품", 10000L, "설명", 50, 3);

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> response =
                testRestTemplate.exchange("/api/v1/products/" + saved.getId(), HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(response.getBody().data().productId()).isEqualTo(saved.getId()),
                () -> assertThat(response.getBody().data().brandId()).isEqualTo(1L),
                () -> assertThat(response.getBody().data().name()).isEqualTo("테스트 상품"),
                () -> assertThat(response.getBody().data().price()).isEqualTo(10000L),
                () -> assertThat(response.getBody().data().description()).isEqualTo("설명"),
                () -> assertThat(response.getBody().data().stockQuantity()).isEqualTo(50),
                () -> assertThat(response.getBody().data().likeCount()).isEqualTo(3)
            );
        }

        @DisplayName("존재하지 않는 상품을 조회하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        void returnsNotFound_whenProductDoesNotExist() {
            // arrange & act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> response =
                testRestTemplate.exchange("/api/v1/products/999", HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                () -> assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.FAIL)
            );
        }
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    class GetProducts {

        @DisplayName("기본 요청 시 latest(id DESC) 정렬로 반환한다.")
        @Test
        void returnsProducts_sortedByLatest() {
            // arrange
            Product p1 = saveProduct(1L, "상품1", 1000L, "설명1", 10);
            Product p2 = saveProduct(1L, "상품2", 2000L, "설명2", 20);
            Product p3 = saveProduct(1L, "상품3", 3000L, "설명3", 30);

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductListResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ProductListResponse>> response =
                testRestTemplate.exchange("/api/v1/products", HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            ProductV1Dto.ProductListResponse data = response.getBody().data();
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(data.products()).hasSize(3),
                () -> assertThat(data.products().get(0).productId()).isEqualTo(p3.getId()),
                () -> assertThat(data.products().get(1).productId()).isEqualTo(p2.getId()),
                () -> assertThat(data.products().get(2).productId()).isEqualTo(p1.getId())
            );
        }

        @DisplayName("sort=likes_desc로 요청 시 likeCount 내림차순으로 반환한다.")
        @Test
        void returnsProducts_sortedByLikesDesc() {
            // arrange
            Product p1 = saveProductWithLikes(1L, "좋아요 적음", 1000L, "설명", 10, 1);
            Product p2 = saveProductWithLikes(1L, "좋아요 많음", 2000L, "설명", 20, 10);
            Product p3 = saveProductWithLikes(1L, "좋아요 중간", 3000L, "설명", 30, 5);

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductListResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ProductListResponse>> response =
                testRestTemplate.exchange("/api/v1/products?sort=likes_desc", HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            ProductV1Dto.ProductListResponse data = response.getBody().data();
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(data.products()).hasSize(3),
                () -> assertThat(data.products().get(0).name()).isEqualTo("좋아요 많음"),
                () -> assertThat(data.products().get(0).likeCount()).isEqualTo(10),
                () -> assertThat(data.products().get(1).name()).isEqualTo("좋아요 중간"),
                () -> assertThat(data.products().get(1).likeCount()).isEqualTo(5),
                () -> assertThat(data.products().get(2).name()).isEqualTo("좋아요 적음"),
                () -> assertThat(data.products().get(2).likeCount()).isEqualTo(1)
            );
        }

        @DisplayName("brandId로 필터링하면, 해당 브랜드 상품만 반환한다.")
        @Test
        void returnsProducts_filteredByBrandId() {
            // arrange
            saveProduct(1L, "브랜드1 상품A", 1000L, "설명", 10);
            saveProduct(1L, "브랜드1 상품B", 2000L, "설명", 20);
            saveProduct(2L, "브랜드2 상품A", 3000L, "설명", 30);

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductListResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ProductListResponse>> response =
                testRestTemplate.exchange("/api/v1/products?brandId=1", HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            ProductV1Dto.ProductListResponse data = response.getBody().data();
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(data.products()).hasSize(2),
                () -> assertThat(data.products()).allMatch(p -> p.brandId().equals(1L)),
                () -> assertThat(data.page().totalElements()).isEqualTo(2)
            );
        }

        @DisplayName("page와 size를 지정하면, 해당 페이지만큼만 반환한다.")
        @Test
        void returnsProducts_withPagination() {
            // arrange
            for (int i = 1; i <= 5; i++) {
                saveProduct(1L, "상품" + i, 1000L * i, "설명", 10);
            }

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductListResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ProductListResponse>> response =
                testRestTemplate.exchange("/api/v1/products?page=0&size=2", HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            ProductV1Dto.ProductListResponse data = response.getBody().data();
            assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(data.products()).hasSize(2),
                () -> assertThat(data.page().page()).isEqualTo(0),
                () -> assertThat(data.page().size()).isEqualTo(2),
                () -> assertThat(data.page().totalElements()).isEqualTo(5),
                () -> assertThat(data.page().totalPages()).isEqualTo(3)
            );
        }
    }

    @DisplayName("상품 상세 조회 캐시 정합성")
    @Nested
    class ProductDetailCacheConsistency {

        private final ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>> productResponseType =
            new ParameterizedTypeReference<>() {};

        private ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> getProduct(Long productId) {
            return testRestTemplate.exchange(
                "/api/v1/products/" + productId, HttpMethod.GET, new HttpEntity<>(null), productResponseType);
        }

        private void likeProduct(Long userId, Long productId) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-UserId", String.valueOf(userId));
            testRestTemplate.exchange(
                "/api/v1/products/" + productId + "/likes", HttpMethod.POST, new HttpEntity<>(null, headers),
                new ParameterizedTypeReference<ApiResponse<Object>>() {});
        }

        private void unlikeProduct(Long userId, Long productId) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-UserId", String.valueOf(userId));
            testRestTemplate.exchange(
                "/api/v1/products/" + productId + "/likes", HttpMethod.DELETE, new HttpEntity<>(null, headers),
                new ParameterizedTypeReference<ApiResponse<Object>>() {});
        }

        @DisplayName("좋아요 등록 후 재조회하면, 캐시가 무효화되어 증가된 likeCount를 반환한다.")
        @Test
        void returnsUpdatedLikeCount_afterLike() {
            // arrange
            Product saved = saveProduct(1L, "캐시 테스트 상품", 10000L, "설명", 50);

            // act - 1차 조회 (캐시 저장)
            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> firstResponse = getProduct(saved.getId());
            assertThat(firstResponse.getBody().data().likeCount()).isEqualTo(0);

            // act - 좋아요 등록 (캐시 evict)
            likeProduct(1L, saved.getId());

            // act - 2차 조회 (캐시 miss → DB에서 최신 데이터 조회)
            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> secondResponse = getProduct(saved.getId());

            // assert
            assertAll(
                () -> assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(secondResponse.getBody().data().likeCount()).isEqualTo(1)
            );
        }

        @DisplayName("좋아요 취소 후 재조회하면, 캐시가 무효화되어 감소된 likeCount를 반환한다.")
        @Test
        void returnsUpdatedLikeCount_afterUnlike() {
            // arrange
            Product saved = saveProduct(1L, "캐시 테스트 상품", 10000L, "설명", 50);
            likeProduct(1L, saved.getId());

            // act - 1차 조회 (캐시 저장, likeCount = 1)
            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> firstResponse = getProduct(saved.getId());
            assertThat(firstResponse.getBody().data().likeCount()).isEqualTo(1);

            // act - 좋아요 취소 (캐시 evict)
            unlikeProduct(1L, saved.getId());

            // act - 2차 조회 (캐시 miss → DB에서 최신 데이터 조회)
            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> secondResponse = getProduct(saved.getId());

            // assert
            assertAll(
                () -> assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(secondResponse.getBody().data().likeCount()).isEqualTo(0)
            );
        }

        @DisplayName("여러 사용자가 좋아요 등록/취소 후에도, 재조회 시 정확한 likeCount를 반환한다.")
        @Test
        void returnsAccurateLikeCount_afterMultipleLikesAndUnlikes() {
            // arrange
            Product saved = saveProduct(1L, "캐시 테스트 상품", 10000L, "설명", 50);

            // act - 3명이 좋아요
            likeProduct(1L, saved.getId());
            likeProduct(2L, saved.getId());
            likeProduct(3L, saved.getId());

            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> afterLikes = getProduct(saved.getId());
            assertThat(afterLikes.getBody().data().likeCount()).isEqualTo(3);

            // act - 1명 취소
            unlikeProduct(2L, saved.getId());

            // act - 재조회
            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> afterUnlike = getProduct(saved.getId());

            // assert
            assertAll(
                () -> assertThat(afterUnlike.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(afterUnlike.getBody().data().likeCount()).isEqualTo(2)
            );
        }
    }
}
