# Commerce Platform - E-Commerce 상품 랭킹 시스템

> 10주간 점진적으로 구축한 **이커머스 플랫폼**입니다.
> 회원/상품/주문 도메인부터 결제, 쿠폰, 대기열, 실시간 랭킹까지 확장하며
> **클린 아키텍처**, **이벤트 기반 설계**, **배치 파이프라인**을 실전 적용했습니다.

---

## Tech Stack

| 구분 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.4.4 |
| Cloud | Spring Cloud | 2024.0.1 |
| Database | MySQL | 8.0 |
| Cache | Redis (Master-Replica) | 7.0 |
| Message Queue | Apache Kafka (KRaft) | 3.5.1 |
| Batch | Spring Batch | 5.x |
| Build | Gradle (Kotlin DSL) | 8.x |
| Test | JUnit 5, Testcontainers, MockK | - |
| Monitoring | Prometheus + Grafana | - |

---

## Architecture

### Multi-Module 구조

```
Root
├── apps ( spring-applications )
│   ├── 📦 commerce-api          ← REST API 서버 (port 8080)
│   ├── 📦 commerce-batch        ← Spring Batch 배치 서버
│   └── 📦 commerce-streamer     ← Kafka Consumer 스트리머
├── modules ( reusable-configurations )
│   ├── 📦 jpa                   ← JPA + QueryDSL + MySQL 설정
│   ├── 📦 redis                 ← Lettuce Master-Replica 설정
│   └── 📦 kafka                 ← Kafka Producer/Consumer 설정
└── supports ( add-ons )
    ├── 📦 jackson               ← 직렬화 설정
    ├── 📦 monitoring            ← Prometheus/Grafana 연동
    └── 📦 logging               ← 구조화 로깅
```

**설계 원칙:**
- `apps`는 실행 가능한 SpringBootApplication으로, BootJar만 생성
- `modules`는 도메인에 의존하지 않는 reusable configuration
- `supports`는 logging, monitoring 등 부가 기능 add-on

### Clean Architecture (각 app 내부)

```
interfaces/     ← Controller, Kafka Listener, Batch Job 설정
    ↓
application/    ← Facade, Use Case 조합 계층
    ↓
domain/         ← Entity, Value Object, Repository 인터페이스, 비즈니스 규칙
    ↓
infrastructure/ ← JPA Repository 구현, Redis 연동, 외부 API 클라이언트
```

### 인프라 구성도

```
┌─────────────┐     ┌─────────────┐     ┌──────────────────┐
│ commerce-api│────▶│   MySQL 8.0 │◀────│ commerce-batch   │
│  (port 8080)│     │  (port 3306)│     │  (Spring Batch)  │
└──────┬──────┘     └─────────────┘     └────────┬─────────┘
       │                                         │
       │            ┌─────────────┐              │
       ├───────────▶│ Redis Master│◀─────────────┤
       │            │  (port 6379)│              │
       │            └──────┬──────┘              │
       │            ┌──────▼──────┐              │
       │            │Redis Replica│              │
       │            │  (port 6380)│              │
       │            └─────────────┘              │
       │                                         │
       │            ┌─────────────┐     ┌────────▼─────────┐
       └───────────▶│ Kafka(KRaft)│◀────│commerce-streamer │
                    │  (port 9092)│     │ (Kafka Consumer)  │
                    └─────────────┘     └──────────────────┘
```

---

## 주차별 구현 내역

### Week 1 - 회원 도메인 & E2E TDD 기반 구축 [PR #37](https://github.com/Loopers-dev-lab/loop-pack-be-l2-vol3-java/pull/37)

**구현 내용:**
- Member 도메인 모델 및 Value Object (`Email`, `Password`, `Nickname`) 구현
- 회원가입 API (`POST /api/v1/members`) - 이메일 중복 검증, BCrypt 비밀번호 암호화
- 내 정보 조회 API (`GET /api/v1/members/me`) - JWT 인증 기반
- 비밀번호 변경 API (`PATCH /api/v1/members/me/password`) - 기존 비밀번호 확인 후 변경
- Testcontainers (MySQL) 기반 E2E 통합 테스트 환경 구성

**고민했던 부분:**
- TDD 사이클을 어떻게 적용할 것인가 → E2E 테스트를 먼저 작성하고 컴파일 에러를 따라가며 구현하는 **Outside-In TDD** 채택. Controller 테스트가 빨간불 → Service → Domain 순으로 내려가며 구현
- 멀티 모듈 환경에서 테스트 인프라 격리 → 각 app 모듈별 `application-test.yml` 분리, Testcontainers로 DB 자동 프로비저닝하여 로컬 환경 의존성 제거

---

### Week 2 - 설계 문서화 & 도메인 모델링 [PR #76](https://github.com/Loopers-dev-lab/loop-pack-be-l2-vol3-java/pull/76)

**구현 내용:**
- 요구사항 정의서 작성 - 회원/상품/주문/쿠폰 도메인별 비즈니스 규칙 정의
- 시퀀스 다이어그램 - 주문 생성, 결제, 쿠폰 적용 흐름 설계
- 클래스 다이어그램 - 도메인 간 연관 관계 및 책임 분리 설계
- ERD 설계 - 테이블 관계, 인덱스 전략, soft delete(`deleted_at`) 컬럼 설계

**고민했던 부분:**
- 주문-상품-쿠폰 간의 의존 방향 설정 → 주문이 상품/쿠폰을 직접 참조하면 양방향 의존 발생 → **도메인 이벤트 기반 느슨한 결합** 방향으로 설계
- 설계 단계에서 확장 가능성 vs 현실적 구현 범위의 균형 → YAGNI 원칙에 따라 현재 요구사항에 충실하되, 이벤트 기반 확장 포인트만 열어둠

---

### Week 3 - 상품/브랜드/좋아요/주문 도메인 구현 [PR #172](https://github.com/Loopers-dev-lab/loop-pack-be-l2-vol3-java/pull/172)

**구현 내용:**
- **Product 도메인**: 재고 차감(`decreaseStock`) / 증가(`increaseStock`) 로직, 재고 부족 시 예외 처리, TDD로 검증
- **Brand 도메인**: `BrandName` Value Object 검증 (길이, 특수문자 제한), BrandService 등록 로직
- **Like 도메인**: 좋아요 등록/취소/카운트 기능, 유저당 1회 제한 (unique constraint)
- **Order 도메인**: 주문 생성 유스케이스 - 상품 재고 차감 → 주문 항목 생성 → 총액 계산
- Like API (`POST /api/v1/likes`, `DELETE /api/v1/likes`) 구현
- 트랜잭션 경계 정리 - Facade 계층에서 `@Transactional` 관리, Domain은 순수 비즈니스 로직만 담당
- Brand 인프라 구현 (JPA Converter, Repository)

**고민했던 부분:**
- 재고 차감 동시성 문제 → 이 시점에서는 도메인 로직의 정합성 검증에 집중, 동시성 제어는 Week 4에서 별도 처리하기로 결정
- 트랜잭션 경계를 어디에 둘 것인가 → Domain 계층에 `@Transactional`을 두면 인프라 의존성 침투 → **Application(Facade) 계층에서 트랜잭션 경계를 관리**하고, Domain은 순수 POJO로 유지

---

### Week 4 - 쿠폰 적용 주문 & 동시성 제어 [PR #183](https://github.com/Loopers-dev-lab/loop-pack-be-l2-vol3-java/pull/183)

**구현 내용:**
- 주문 시 쿠폰 적용 로직 - 쿠폰 유효성 검증 (만료일, 사용 여부, 최소 주문금액) → 할인 금액 계산 → 주문 총액에 반영
- 주문 정합성 처리 - 재고 차감 + 쿠폰 사용 + 주문 생성이 하나의 트랜잭션에서 원자적 처리
- 좋아요 동시성 제어 - 낙관적 락(`@Version`) + retry 메커니즘 적용
- 동시성 통합 테스트 - `ExecutorService` + `CountDownLatch`로 다수 스레드 동시 요청 검증

**고민했던 부분:**
- 좋아요 동시성 제어 방식 선택 → **비관적 락**: 처리량 저하 + DB 커넥션 점유 시간 증가 / **낙관적 락**: retry 로직 복잡도 증가 → 좋아요는 충돌 빈도가 낮아 **낙관적 락 + retry** 채택
- 쿠폰 적용과 재고 차감의 트랜잭션 범위 → 쿠폰만 사용되고 재고 차감 실패 시 데이터 불일치 발생 → 하나의 트랜잭션으로 묶어 all-or-nothing 보장

---

### Week 5 - 상품 조회 성능 최적화 & Redis 캐시 [PR #216](https://github.com/Loopers-dev-lab/loop-pack-be-l2-vol3-java/pull/216)

**구현 내용:**
- 상품 목록 조회 API - 좋아요 수 반영 정렬 구조, 커서 기반 페이지네이션
- **인덱스 최적화**: 상품 조회 쿼리에 복합 인덱스 설계, 대용량(10만건+) 데이터 성능 검증
- **Redis 캐시 적용**: 상품 상세 조회에 Look-Aside 캐시 패턴 적용
- 캐시-DB 정합성 검증 테스트 - 상품 수정 후 캐시 무효화 확인

**고민했던 부분:**
- 좋아요 수를 조회 시 어떻게 효율적으로 반영할 것인가 → 매번 JOIN은 N+1 문제 발생 → **비정규화 카운터 + Redis 캐시**로 읽기 부하 분산
- 캐시 무효화 전략 → Write-Through vs TTL 기반 → 상품 상세는 실시간 정합성보다 조회 성능이 중요하므로 **TTL 기반 eventual consistency** 허용
- 커버링 인덱스 vs 복합 인덱스 → 조회 컬럼이 많아 커버링 인덱스는 비효율적 → WHERE + ORDER BY 절에 맞춘 복합 인덱스로 결정

---

### Week 6 - 결제 시스템 & Resilience 패턴 [PR #237](https://github.com/Loopers-dev-lab/loop-pack-be-l2-vol3-java/pull/237)

**구현 내용:**
- **주문/결제 상태 전이 모델**: State Machine 패턴으로 `PENDING → PAID → CANCELLED` 상태 흐름 관리, 잘못된 전이 시 예외
- **PG 연동 인터페이스**: `PgClient` 인터페이스 + PG Simulator 클라이언트 구현 (외부 의존성 추상화)
- **결제 오케스트레이션 (Saga 패턴)**: 결제 요청 → PG 승인 → 주문 상태 변경, 실패 시 보상 트랜잭션 (결제 취소 → 재고 복구)
- **PG Callback 처리**: 비동기 PG 응답 수신 → 결제 상태 반영
- **결제 복구 스케줄러**: PENDING 상태 타임아웃(5분) 감지 → PG 상태 조회 → 자동 보상 처리
- **Resilience4j 적용**: PG 호출에 `Bulkhead` (maxConcurrentCalls) 설정으로 동시 호출 제한
- 결제 동시성 통합 테스트 - PG callback과 사용자 요청 동시 도달 시나리오 검증

**고민했던 부분:**
- 결제 실패 보상 처리 방식 → **Choreography Saga**: 이벤트 기반으로 각 서비스가 독립적 보상 / **Orchestration Saga**: 중앙 조율자가 보상 흐름 관리 → 결제 도메인이 전체 흐름의 중심이므로 **Orchestration Saga** 채택
- PG callback과 사용자 취소 요청의 동시 도달 → 결제 상태 전이에 **비관적 락(`SELECT FOR UPDATE`)** 적용으로 race condition 방지
- Resilience4j 설정값 결정 → PG 평균 응답시간과 서버 스레드풀 크기를 고려하여 Bulkhead `maxConcurrentCalls`와 `maxWaitDuration` 설정

---

### Week 7 - 이벤트 기반 아키텍처 & 선착순 쿠폰 [PR #293](https://github.com/Loopers-dev-lab/loop-pack-be-l2-vol3-java/pull/293)

**구현 내용:**
- **도메인 이벤트 발행**: `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 비동기 핸들러
- **Transactional Outbox 패턴**:
  - `OutboxEvent` 엔티티 (eventType enum, payload JSON, status, partitionKey)
  - Scheduler가 미발행 이벤트를 polling → Kafka로 relay
  - Consumer 측 `IdempotencyKey` 테이블로 중복 소비 방어
- **Kafka 설정 강화**: Producer `acks=all` + `enable.idempotence=true`, Consumer `manual ack` + offset commit 보완
- **선착순 쿠폰 시스템**:
  - `CouponIssueRequest` 도메인 모델 (PENDING → ISSUED / FAILED 상태)
  - Redis `DECR`로 쿠폰 재고 원자적 차감
  - Kafka Consumer에서 비동기 발급 처리
  - 발급 요청 API (`POST /api/v1/coupons/issue`) + polling 조회 API
- **OutboxEvent partitionKey**: orderId 기반 Kafka partition 전략으로 동일 주문 이벤트 순서 보장
- 쿠폰 발급 동시성 테스트 및 Kafka 관련 테스트 보강

**고민했던 부분:**
- 이벤트 발행과 DB 트랜잭션의 원자성 → `@TransactionalEventListener(AFTER_COMMIT)`만으로는 커밋 후 이벤트 발행 실패 시 유실 → **Outbox 패턴으로 at-least-once delivery 보장**, Consumer에서 멱등 처리로 exactly-once semantics 근사
- Kafka partition 전략 → 같은 주문의 이벤트(생성/결제/취소)는 순서 보장 필요 → `orderId`를 partitionKey로 사용하여 동일 파티션 라우팅
- 선착순 쿠폰 재고의 원자적 차감 → DB 락은 병목 → **Redis DECR**로 원자적 차감 후, 실제 쿠폰 발급은 Kafka 비동기 처리로 분리

---

### Week 8 - Redis 기반 주문 대기열 시스템 [PR #335](https://github.com/Loopers-dev-lab/loop-pack-be-l2-vol3-java/pull/335)

**구현 내용:**
- **Redis Sorted Set 대기열**: score를 timestamp로 사용하여 FIFO 순서 보장
- 대기열 진입 API (`POST /api/v1/queue/enter`) - 중복 진입 방지 (NX 옵션)
- 대기 상태 조회 API - 현재 대기 순번 + 예상 대기시간 계산
- 토큰 검증 미들웨어 - 주문 API 호출 시 유효한 입장 토큰 보유 여부 확인
- **Lua Script 원자 연산**: `ZPOPMIN` (대기열 dequeue) + `SET NX EX` (토큰 발급)를 하나의 스크립트로 묶어 원자 실행
- 주문 성공 시 입장 토큰 명시적 삭제 + TTL fallback 이중 전략
- 활성 토큰 보유 유저의 대기열 재진입 차단
- 동시성 테스트: 2000명 동시 진입, 처리량 초과 검증, 토큰 TTL 만료 시나리오

**고민했던 부분:**
- dequeue + 토큰 발급의 원자성 → 두 Redis 명령을 순차 실행하면 중간 장애 시 토큰 없이 대기열에서 빠져나가는 유저 발생 → **Lua Script**로 `ZPOPMIN + SET NX EX`를 단일 원자 연산으로 묶어 해결
- 토큰 TTL 설정 → 너무 짧으면(1분) 정상 사용자도 주문 중 만료, 너무 길면(1시간) 좀비 토큰 누적 → **주문 완료 시 명시적 DEL + TTL(5분) fallback** 이중 전략으로 리소스 누수 방지
- 예상 대기시간 계산 정확도 → 단순 `순번 × 평균처리시간`은 부정확 → 최근 N건의 처리 속도를 기반으로 동적 계산하도록 보완

---

### Week 9 - 실시간 상품 랭킹 파이프라인 [PR #383](https://github.com/Loopers-dev-lab/loop-pack-be-l2-vol3-java/pull/383)

**구현 내용:**
- **Ranking Score Policy**: 이벤트 유형별 가중치 기반 점수 산출 (주문 완료, 좋아요, 조회 등)
- **Kafka → Redis 실시간 파이프라인**: Kafka 배치 Consumer (`max.poll.records=3000`)가 이벤트 소비 → Score Policy 적용 → Redis ZSET(`ranking:all:{yyyyMMdd}`) `ZINCRBY`로 실시간 적재
- **ZUNIONSTORE 기반 carry-over**: 자정에 전일 랭킹 데이터를 가중치(0.5)를 낮춰 새 날짜 키로 복사, 콜드 스타트 완화
- Carry-over Scheduler - 매일 자정 자동 실행, 테스트로 동작 검증
- 상품 랭킹 조회 API (`GET /api/v1/rankings`) - Redis ZREVRANGE로 TOP N 조회
- 상품 상세 조회에 dailyRank 필드 추가

**고민했던 부분:**
- 콜드 스타트 문제 → 자정에 새로운 날짜 키(`ranking:all:20250615`)가 생성되면 데이터가 비어있음 → **`ZUNIONSTORE`로 전일 데이터를 가중치(0.5)로 carry-over**, 당일 이벤트가 쌓이면 자연스럽게 전일 영향 감소
- Redis 키 네이밍 전략 → `ranking:{category}:{yyyyMMdd}` 형태로 일자별 분리, TTL(3일)로 오래된 키 자동 정리
- Kafka 배치 Consumer 설정 → `max.poll.records=3000`, `session.timeout.ms=60000`, manual ack으로 처리량 확보와 유실 방지 균형

---

### Week 10 - Spring Batch 랭킹 집계 & API 확장 [PR #420](https://github.com/Loopers-dev-lab/loop-pack-be-l2-vol3-java/pull/420)

**구현 내용:**
- **Daily Metrics Snapshot Batch** (`dailyMetricsSnapshotJob`):
  - Redis ZSET 전체 스코어 조회 → `product_metrics` 테이블에 일별 스냅샷 저장
  - `EntityManager.flush()/clear()` 1000건마다 호출하여 영속성 컨텍스트 메모리 제어
  - Empty result guard - Redis에 데이터가 없으면 기존 DB 데이터 보호 (삭제 스킵)
  - 잘못된 productId(null, 음수) 필터링
- **Weekly/Monthly Rank Aggregation Batch** (`rankAggregationJob`):
  - `JdbcCursorItemReader`로 `product_metrics` 7일/30일 구간 집계 (SUM + GROUP BY + ORDER BY)
  - TOP 100 제한으로 불필요한 연산 방지
  - Chunk 기반 처리 (chunk size: 100)
  - `WeeklyRankWriter` / `MonthlyRankWriter` - 첫 번째 chunk에서 `AtomicBoolean`으로 기존 데이터 1회 삭제 후 insert
  - `mv_product_rank_weekly` / `mv_product_rank_monthly` 테이블에 적재
- **Ranking API 확장** (`GET /api/v1/rankings?period=DAILY|WEEKLY|MONTHLY`):
  - `RankPeriod` enum으로 기간별 분기
  - DAILY: `product_metrics` 직접 조회 (score DESC)
  - WEEKLY/MONTHLY: materialized view 테이블 조회 (ranking ASC)
  - 페이지 크기 제한 (max 100)
- **Batch 모니터링**: `JobListener`, `StepMonitorListener`, `ChunkListener`로 실행 메트릭 로깅
- 모든 배치 작업 **멱등성 보장** - 동일 `requestDate` 파라미터로 재실행 시 동일 결과 (`deleteByDate` → `saveAll`)
- E2E 테스트: 집계 정확성, 날짜 범위 필터링, TOP 100 제한, 멱등성, 엣지 케이스

**고민했던 부분:**
- Redis 스냅샷을 왜 MySQL에 저장하는가 → Redis는 휘발성이고 장기 데이터 보관에 부적합 → MySQL에 일별 스냅샷을 남겨 **주간/월간 집계의 안정적인 원천 데이터** 확보
- 대량 insert 시 메모리 관리 → JPA `persist()`가 1차 캐시에 엔티티를 누적해 OOM 위험 → **1000건마다 `flush() + clear()`** 호출로 메모리 사용량 일정하게 유지
- Empty result guard → Redis 장애나 데이터 부재 시 배치가 기존 DB 데이터를 삭제하면 안됨 → Redis 결과가 비어있으면 **기존 데이터 보호 후 스킵**
- 주간/월간 랭킹 테이블 전략 → DB View는 조회마다 집계 연산 발생으로 느림 → **Materialized View 전략** (물리 테이블 `mv_product_rank_weekly/monthly`에 배치로 미리 적재)으로 조회 성능 확보
- Writer의 delete 타이밍 → 모든 chunk마다 delete하면 데이터 유실 → `AtomicBoolean`으로 **첫 번째 chunk에서만 1회 삭제**, 이후 chunk는 insert만 수행

---

## 전체 데이터 흐름

```
[사용자 주문/이벤트]
       │
       ▼
┌──────────────┐    Kafka Event     ┌──────────────────┐
│ commerce-api │ ──────────────────▶ │ commerce-streamer│
│  (Outbox)    │                    │ (Kafka Consumer) │
└──────────────┘                    └────────┬─────────┘
                                             │
                                    Score Policy 적용
                                             │
                                             ▼
                                    ┌────────────────┐
                                    │  Redis ZSET    │
                                    │ (일별 실시간   │
                                    │  랭킹 스코어)  │
                                    └────────┬───────┘
                                             │
                              Daily Snapshot Batch
                                             │
                                             ▼
                                    ┌────────────────┐
                                    │ product_metrics│
                                    │  (MySQL 일별)  │
                                    └────────┬───────┘
                                             │
                           Rank Aggregation Batch (7일/30일)
                                             │
                                    ┌────────▼───────┐
                                    │ mv_product_rank│
                                    │ _weekly/monthly│
                                    └────────┬───────┘
                                             │
                                    Ranking API 조회
                                             │
                                             ▼
                                    ┌────────────────┐
                                    │   클라이언트    │
                                    └────────────────┘
```

---

## 핵심 설계 결정 요약

| 주제 | 결정 | 이유 |
|------|------|------|
| 아키텍처 | Clean Architecture + 멀티 모듈 | 도메인 로직 보호, 인프라 교체 용이성 |
| 트랜잭션 경계 | Application(Facade) 계층 | Domain 계층의 인프라 의존성 제거 |
| 동시성 제어 | 낙관적 락 + retry (좋아요), 비관적 락 (결제) | 유스케이스별 충돌 빈도에 따라 전략 분리 |
| 이벤트 발행 | Transactional Outbox 패턴 | DB 트랜잭션과 메시지 발행의 원자성 보장 |
| 대기열 원자성 | Redis Lua Script | dequeue + 토큰 발급을 단일 원자 연산으로 |
| 랭킹 콜드 스타트 | ZUNIONSTORE carry-over | 자정 랭킹 초기화 문제 해결 |
| 장기 랭킹 집계 | Materialized View 전략 | Redis 휘발성 극복, 주간/월간 안정적 집계 |
| 배치 메모리 | flush/clear per 1000건 | 대량 insert 시 영속성 컨텍스트 OOM 방지 |
| 결제 보상 | Orchestration Saga | 결제 중심의 명확한 보상 흐름 |
| 캐시 전략 | TTL 기반 Look-Aside | 상품 상세 읽기 부하 분산 |
| Kafka 신뢰성 | acks=all + manual ack + 멱등 | 메시지 유실 방지 + 중복 소비 방어 |
| Redis 가용성 | Master-Replica + ReadFrom 분리 | 읽기 부하 분산, replica fallback |

---

## Getting Started

### Environment
`local` 프로필로 동작할 수 있도록, 필요 인프라를 `docker-compose`로 제공합니다.
```shell
docker-compose -f ./docker/infra-compose.yml up
```

### Monitoring
`local` 환경에서 모니터링을 할 수 있도록, `prometheus`와 `grafana`를 제공합니다.

애플리케이션 실행 이후, **http://localhost:3000** 로 접속해, admin/admin 계정으로 로그인하여 확인하실 수 있습니다.
```shell
docker-compose -f ./docker/monitoring-compose.yml up
```

### API Documentation
애플리케이션 실행 후 **http://localhost:8080/swagger-ui.html** 에서 API 문서를 확인할 수 있습니다.
