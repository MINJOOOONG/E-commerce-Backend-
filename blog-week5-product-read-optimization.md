# 좋아요 순 정렬을 만들다가 쿼리 73배 빨라진 이야기 — 비정규화, 인덱스, Redis 캐시까지

## 제목 후보

1. **좋아요 순 정렬을 만들다가 쿼리 73배 빨라진 이야기 — 비정규화, 인덱스, Redis 캐시까지**
2. **상품 조회 API에 인덱스와 캐시를 적용하며 배운 것들 — 20ms → 0.28ms의 기록**
3. **"정렬 하나 추가해주세요" 에서 시작된 조회 성능 최적화 여정**

---

## 들어가며

"상품 목록을 좋아요 많은 순으로 정렬해주세요."

요구사항 자체는 한 줄이었다. 그런데 이 한 줄을 구현하면서 비정규화 설계, 10만 건 데이터 기반 인덱스 튜닝, Redis 캐시 적용까지 꽤 긴 여정을 거쳤다. 이번 글은 그 과정을 순서대로 정리한 기록이다.

---

## 1. 처음 문제 — 좋아요 순 정렬을 어떻게 할 것인가

기존 시스템에는 `likes` 테이블이 따로 있었다. 사용자가 상품에 좋아요를 누르면 `(userId, productId)` 조합으로 한 행이 생기는 구조다.

이 상태에서 "좋아요 많은 순 정렬"을 하려면?

```sql
SELECT p.*, COUNT(l.id) AS like_count
FROM products p
LEFT JOIN likes l ON p.id = l.product_id
GROUP BY p.id
ORDER BY like_count DESC
LIMIT 20;
```

데이터가 적을 때는 문제없지만, 상품 10만 건 × 좋아요 수십만 건이 되면 매 요청마다 JOIN + GROUP BY + ORDER BY를 돌리게 된다. 페이지네이션까지 붙으면 더 심해진다.

### 선택: Product 엔티티에 likeCount 비정규화

고민 끝에 `Product` 엔티티에 `likeCount` 필드를 직접 넣기로 했다.

```java
@Column(name = "like_count", nullable = false)
private long likeCount = 0L;

public void increaseLikeCount() {
    this.likeCount++;
}

public void decreaseLikeCount() {
    if (this.likeCount <= 0) {
        throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 미만이 될 수 없습니다");
    }
    this.likeCount--;
}
```

비정규화는 항상 트레이드오프가 있다. 정합성 관리 부담이 생기는 대신, 조회 시점에 JOIN 없이 단일 테이블만으로 정렬이 가능해진다. 좋아요 순 정렬은 조회 빈도가 높은 반면 좋아요 변경은 상대적으로 적기 때문에 이 방향이 맞다고 판단했다.

좋아요/취소 시 동기화는 `LikeFacade`에서 비관적 락과 함께 처리한다.

```java
@Transactional
public LikeInfo like(Long userId, Long productId) {
    Product product = getProductWithLock(productId);
    boolean alreadyLiked = likeRepository.findByUserIdAndProductId(userId, productId).isPresent();

    Like like = likeService.like(userId, productId);

    if (!alreadyLiked) {
        product.increaseLikeCount();
    }

    return LikeInfo.from(like);
}
```

---

## 2. 상품 조회 API 구현

비정규화된 likeCount를 기반으로 상품 조회 API를 만들었다.

### 상세 조회

```
GET /api/v1/products/{productId}
```

### 목록 조회

```
GET /api/v1/products?brandId=1&sort=likes_desc&page=0&size=20
```

| 파라미터 | 설명 | 기본값 |
|----------|------|--------|
| `brandId` | 브랜드 필터 (optional) | 전체 |
| `sort` | 정렬 방식 (`latest`, `likes_desc`) | `latest` |
| `page` | 페이지 번호 | 0 |
| `size` | 페이지 크기 | 20 |

정렬 로직은 `ProductFacade`에서 `Sort` 객체로 변환한다.

```java
private Sort resolveSort(String sort) {
    if ("likes_desc".equals(sort)) {
        return Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("id"));
    }
    if (sort == null || "latest".equals(sort)) {
        return Sort.by(Sort.Order.desc("id"));
    }
    throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 정렬 방식입니다: " + sort);
}
```

`likes_desc` 정렬에서 `id DESC`를 보조 정렬로 넣은 이유는, likeCount가 같은 상품끼리는 최신순으로 정렬하기 위함이다. 이 순서는 나중에 인덱스 설계에도 그대로 반영된다.

### ✅ 여기까지 구현 체크리스트

- [x] Product 엔티티에 likeCount 비정규화
- [x] 좋아요/취소 시 likeCount 동기화 (비관적 락)
- [x] 상품 상세 조회 API (`GET /api/v1/products/{id}`)
- [x] 상품 목록 조회 API (`GET /api/v1/products`)
- [x] latest / likes_desc 정렬
- [x] brandId 필터링
- [x] 페이지네이션

---

## 3. 10만 건 테스트 데이터 준비

API가 동작하는 건 확인했지만, 실제 성능은 데이터가 충분해야 의미 있다. 10만 건 상품 데이터를 넣어서 확인하기로 했다.

### 왜 JPA save가 아니라 SQL bulk insert인가

JPA로 10만 건을 넣으면?

- `new Product()` → `em.persist()` → dirty checking → flush → 10만 번 INSERT
- 엔티티 생성자 검증, `@PrePersist` 콜백까지 다 타면서 **수십 분** 소요

반면 raw SQL INSERT는 5,000건씩 묶어서 20번이면 끝난다. 시드 데이터는 비즈니스 검증이 필요 없으니 SQL이 합리적이다.

```python
TOTAL = 100_000
CHUNK_SIZE = 5000
BRAND_COUNT = 20
MAX_LIKES = 5000

for start in range(0, TOTAL, CHUNK_SIZE):
    end = min(start + CHUNK_SIZE, TOTAL)
    print("INSERT INTO products (brand_id, name, price, description, "
          "stock_quantity, like_count, created_at, updated_at, deleted_at) VALUES")
    vals = []
    for i in range(start + 1, end + 1):
        brand_id = random.randint(1, BRAND_COUNT)
        name = f'상품_{i:06d}'
        price = random.randint(1000, 100000)
        stock = random.randint(0, 1000)
        like_count = random.randint(0, MAX_LIKES)
        vals.append(f"({brand_id},'{name}',{price},'설명_{i}',{stock},{like_count},...)")
    print(",\n".join(vals) + ";")
```

| 항목 | 값 |
|------|-----|
| 총 상품 수 | 100,000건 |
| brand_id 분포 | 1~20 균등 |
| like_count 범위 | 0~5,000 랜덤 |
| price 범위 | 1,000~100,000 랜덤 |
| INSERT 단위 | 5,000건씩 20회 |

---

## 4. 인덱스 적용 전 — EXPLAIN으로 현실 확인

데이터를 넣고, 실제 API에서 사용하는 쿼리 패턴 3가지를 EXPLAIN으로 분석했다.

### 분석 대상 쿼리

| 쿼리 패턴 | 설명 |
|-----------|------|
| `brandId=5&sort=likes_desc` | 특정 브랜드 + 좋아요순 |
| `sort=likes_desc` (전체) | 전체 상품 좋아요순 |
| `brandId=5&sort=latest` | 특정 브랜드 + 최신순 |

### 인덱스 적용 전 결과

| 쿼리 | 실행 시간 | type | Extra |
|------|-----------|------|-------|
| brandId + likes_desc | **20.4ms** | ALL | Using where; Using filesort |
| 전체 likes_desc | **26.7ms** | ALL | Using filesort |
| brandId + latest | **0.4ms** | ALL | Using where |

`ALL` = 풀 테이블 스캔, `filesort` = 별도 정렬 작업. 10만 건 전체를 읽고 정렬까지 하고 있었다.

`brandId + latest`가 비교적 빠른 이유는 PK(id) 역순 정렬이 클러스터 인덱스와 일치하기 때문이다. 하지만 이것도 풀 스캔은 풀 스캔이다.

---

## 5. 인덱스 설계

쿼리 패턴에 맞춰 복합 인덱스 2개를 설계했다.

```java
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_brand_like", columnList = "brand_id, like_count DESC, id DESC"),
    @Index(name = "idx_products_like", columnList = "like_count DESC, id DESC")
})
public class Product extends BaseEntity {
    // ...
}
```

### 왜 이 순서인가?

**`idx_products_brand_like`**: `(brand_id, like_count DESC, id DESC)`

1. `brand_id` — WHERE 절에서 동등 조건으로 필터링
2. `like_count DESC` — 필터링 후 정렬 (filesort 제거)
3. `id DESC` — likeCount 동률 시 보조 정렬 + 커버링 인덱스 효과

**`idx_products_like`**: `(like_count DESC, id DESC)`

- brandId 필터 없이 전체 상품을 좋아요순으로 조회할 때 사용
- brand_id가 없으므로 2컬럼만으로 구성

핵심은 **WHERE 조건 → ORDER BY 순서**로 인덱스 컬럼을 배치하는 것이다. 이 순서가 뒤바뀌면 인덱스를 타도 filesort가 발생한다.

### ✅ 여기까지 성능 최적화 체크리스트

- [x] 10만 건 테스트 데이터 생성 (Python SQL bulk insert)
- [x] 인덱스 적용 전 EXPLAIN 분석 (풀스캔 + filesort 확인)
- [x] 쿼리 패턴 기반 복합 인덱스 2개 설계
- [x] `@Table(indexes = ...)` 로 JPA 엔티티에 반영

---

## 6. 인덱스 적용 후 — 결과

### 적용 후 EXPLAIN 결과

| 쿼리 | 실행 시간 | type | Extra |
|------|-----------|------|-------|
| brandId + likes_desc | **0.28ms** | ref | Backward index scan |
| 전체 likes_desc | **0.09ms** | index | Backward index scan |
| brandId + latest | **0.16ms** | ref | Backward index scan |

### 개선율

| 쿼리 | Before | After | 개선율 |
|------|--------|-------|--------|
| brandId + likes_desc | 20.4ms | 0.28ms | **73배** |
| 전체 likes_desc | 26.7ms | 0.09ms | **297배** |
| brandId + latest | 0.4ms | 0.16ms | **2.5배** |

`ALL` → `ref` 또는 `index`로 바뀌었고, `filesort`가 완전히 사라졌다. 인덱스만으로 정렬된 순서를 바로 읽어오는 `Backward index scan`으로 전환된 것이다.

---

## 7. Redis 캐시 적용

인덱스로 쿼리 자체는 충분히 빨라졌지만, 트래픽이 많아지면 DB 커넥션 자체가 병목이 된다. 상품 상세 조회에 Redis 캐시를 적용하기로 했다.

### 왜 상세 조회부터?

| 기준 | 상세 조회 | 목록 조회 |
|------|-----------|-----------|
| 캐시 키 | `product::1` (단순) | `products:brand:1:sort:likes_desc:page:0:size:20` (조합 폭발) |
| 무효화 범위 | 해당 상품 1건만 evict | brandId별 + 전체 목록 다수 evict |
| 히트율 | 같은 상품 반복 조회 → 높음 | 페이지/정렬 조합 다양 → 낮음 |

목록 조회는 캐시 키 조합이 너무 많고, likeCount 변경 시 어떤 키를 무효화해야 할지 범위가 넓어서 이번에는 적용하지 않았다.

### CacheManager 설정

```java
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .entryTtl(Duration.ofMinutes(5))
            .disableCachingNullValues();

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .build();
    }
}
```

| 설정 | 값 | 이유 |
|------|-----|------|
| Value Serializer | `GenericJackson2JsonRedisSerializer` | ProductInfo(record) → JSON 직렬화 |
| TTL | 5분 | 상품 정보 갱신 빈도 고려 |
| Null 캐싱 | 비활성화 | 존재하지 않는 상품이 캐시되는 것 방지 |

### @Cacheable — 상세 조회

```java
@Cacheable(cacheNames = "product", key = "#productId")
@Transactional(readOnly = true)
public ProductInfo getProduct(Long productId) {
    Product product = productService.getProductById(productId);
    return ProductInfo.from(product);
}
```

캐시 히트 시 `@Transactional` 진입 자체를 건너뛰기 때문에 DB 커넥션도 사용하지 않는다.

### @CacheEvict — 좋아요/취소

```java
@CacheEvict(cacheNames = "product", key = "#productId")
@Transactional
public LikeInfo like(Long userId, Long productId) {
    // ... 좋아요 처리 + likeCount 증가
}

@CacheEvict(cacheNames = "product", key = "#productId")
@Transactional
public void unlike(Long userId, Long productId) {
    // ... 좋아요 취소 + likeCount 감소
}
```

좋아요가 변경되면 해당 상품의 캐시를 삭제한다. 다음 조회 시 캐시 miss → DB에서 최신 데이터를 읽어온다.

### 캐시 흐름 정리

```
[조회] GET /api/v1/products/1
  → @Cacheable 확인
    → 캐시 있음 → Redis에서 반환 (DB 접근 없음)
    → 캐시 없음 → DB 조회 → 응답 + Redis 저장

[좋아요] POST /api/v1/products/1/likes
  → DB에서 likeCount 변경 (비관적 락)
  → @CacheEvict → product::1 삭제
  → 다음 조회 시 캐시 miss → 최신 데이터 반환
```

### ✅ 여기까지 캐시 적용 체크리스트

- [x] RedisCacheManager 등록 (JSON Serializer + TTL 5분)
- [x] `@EnableCaching` 활성화
- [x] `ProductFacade.getProduct()`에 `@Cacheable` 적용
- [x] `LikeFacade.like/unlike()`에 `@CacheEvict` 적용
- [x] 목록 조회 캐시는 의도적으로 미적용 (무효화 범위 문제)

---

## 8. 캐시 정합성 E2E 테스트

캐시를 적용하면 "DB는 바뀌었는데 캐시는 안 바뀐" 상황이 가장 위험하다. 이걸 E2E 레벨에서 검증했다.

### 시나리오 1: 좋아요 후 재조회

```
1. 상품 조회 → likeCount = 0 (캐시 저장)
2. 좋아요 등록 → DB likeCount = 1 + 캐시 evict
3. 상품 재조회 → likeCount = 1 (캐시 miss → DB 조회)
```

### 시나리오 2: 좋아요 취소 후 재조회

```
1. 좋아요 등록
2. 상품 조회 → likeCount = 1 (캐시 저장)
3. 좋아요 취소 → DB likeCount = 0 + 캐시 evict
4. 상품 재조회 → likeCount = 0 (캐시 miss → DB 조회)
```

### 시나리오 3: 여러 사용자 좋아요/취소 혼합

```
1. 3명 좋아요 → likeCount = 3
2. 1명 취소 → likeCount = 2
3. 재조회 → likeCount = 2 확인
```

세 시나리오 모두 E2E 테스트로 작성했고, `@AfterEach`에서 DB + Redis 모두 초기화하여 테스트 격리를 보장한다.

```java
@AfterEach
void tearDown() {
    databaseCleanUp.truncateAllTables();
    redisCleanUp.truncateAll();
}
```

---

## 9. 이번 작업에서 배운 점

### 인덱스는 "추가"가 아니라 "쿼리와 같이 설계"해야 한다

인덱스를 나중에 얹는 게 아니라, 쿼리의 WHERE 조건과 ORDER BY 순서에 맞춰서 설계해야 한다. `brand_id → like_count DESC → id DESC` 순서를 하나라도 바꾸면 filesort가 다시 발생한다.

### 캐시는 모든 조회에 넣는 게 아니라 무효화 범위까지 봐야 한다

상세 조회는 `product::1` 하나만 evict하면 된다. 목록 조회는 brandId × sort × page × size 조합만큼 캐시 키가 생기고, likeCount 하나 바뀌면 어떤 키를 invalidate해야 하는지 판단이 어렵다. "캐시하기 쉬운 것"이 아니라 "무효화하기 쉬운 것"부터 적용하는 게 맞다.

### 성능 최적화는 실제 데이터 크기에서 검증해야 의미 있다

100건으로는 풀 스캔이든 인덱스든 차이가 안 난다. 10만 건을 넣고 나서야 20ms → 0.28ms라는 차이가 드러났다. 그래서 시드 스크립트를 먼저 만들고, EXPLAIN으로 확인한 다음에 인덱스를 설계한 순서가 결과적으로 맞았다.

---

## 전체 구현 체크리스트

### 구현

- [x] Product.likeCount 비정규화
- [x] 좋아요/취소 시 likeCount 동기화 (비관적 락 + 멱등성)
- [x] 상품 상세 조회 API
- [x] 상품 목록 조회 API (정렬, 필터, 페이지네이션)
- [x] E2E 테스트 (상세 조회, 목록 조회 6건)

### 성능 최적화

- [x] 10만 건 시드 데이터 생성
- [x] EXPLAIN 기반 쿼리 분석
- [x] 복합 인덱스 2개 설계 및 적용
- [x] 73배~297배 성능 개선 확인

### 캐시 적용

- [x] RedisCacheManager + JSON Serializer 설정
- [x] 상세 조회 @Cacheable (TTL 5분)
- [x] 좋아요/취소 @CacheEvict
- [x] 캐시 정합성 E2E 테스트 3건
- [ ] 목록 조회 캐시 (다음 단계)
