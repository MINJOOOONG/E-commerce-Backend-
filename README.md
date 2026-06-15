# Commerce Platform - E-Commerce 상품 랭킹 시스템

> Loopers L2 Vol.3 과정에서 주차별로 점진적으로 구축한 **이커머스 플랫폼**입니다.
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

### Week 1 - 회원 도메인 & TDD 기반 구축

**구현 내용:**
- 회원가입 / 내 정보 조회 API (E2E TDD)
- Clean Architecture 레이어 분리 (interfaces → application → domain → infrastructure)
- Testcontainers 기반 통합 테스트 환경 구성

**고민했던 부분:**
- TDD Red-Green-Refactor 사이클을 얼마나 엄격하게 지킬 것인가 → E2E 테스트를 먼저 작성하고 컴파일 에러를 따라가며 구현하는 Outside-In 방식 채택
- 멀티 모듈에서 테스트 프로필 관리 → 각 app 모듈별 `application-test.yml` 분리, Testcontainers로 인프라 격리

---

### Week 2 - 설계 문서화

**구현 내용:**
- 요구사항 정의서 작성 및 정책 확정
- 시퀀스 다이어그램, 클래스 다이어그램, ERD 설계
- 도메인 간 관계와 책임 경계 정의

**고민했던 부분:**
- 주문-상품-쿠폰 간의 의존 방향을 어떻게 설정할 것인가 → 도메인 이벤트 기반으로 느슨한 결합 방향 결정
- 설계 단계에서 확장 가능성과 현실적 구현 범위의 균형

---

### Week 3 - 상품/주문/좋아요 도메인 구현

**구현 내용:**
- Product 재고 차감/증가 로직 (TDD)
- Brand 도메인 및 BrandName Value Object 검증
- Like(좋아요) 등록/취소/카운트 기능
- Order(주문) 생성 유스케이스 및 API v1
- 트랜잭션 경계 정리 및 명시적 save

**고민했던 부분:**
- 재고 차감 시 동시성 문제 → 이 시점에서는 도메인 로직 정합성에 집중하고, 동시성 제어는 이후 주차에서 해결
- 트랜잭션 경계를 어디에 둘 것인가 → Application(Facade) 계층에서 `@Transactional`을 관리하고, Domain 계층은 순수 비즈니스 로직만 담당

---

### Week 4 - 쿠폰 적용 & 동시성 제어

**구현 내용:**
- 주문 시 쿠폰 적용 및 정합성 처리
- 좋아요 동시성 제어 (비관적 락 / 낙관적 락 검토)
- 동시성 통합 테스트 추가

**고민했던 부분:**
- 좋아요 카운트의 동시성 제어 방식 → 비관적 락은 처리량 저하 우려, 낙관적 락은 retry 로직 복잡도 증가 → 유스케이스 특성상 충돌 빈도가 낮아 낙관적 락 + retry 채택
- 쿠폰 적용과 주문 생성이 하나의 트랜잭션에서 처리되어야 하는 이유와 경계 설정

---

### Week 5 - 상품 조회 성능 최적화

**구현 내용:**
- 상품 조회 API 및 좋아요 수 반영 구조
- 상품 조회 인덱스 설계 및 대용량 성능 검증
- 상품 상세 Redis 캐시 적용 및 캐시-DB 정합성 검증

**고민했던 부분:**
- 좋아요 수를 상품 조회 시 어떻게 효율적으로 반영할 것인가 → 조회 시마다 JOIN vs 비정규화 카운터 → Redis 캐시로 읽기 부하 분산
- 캐시 무효화 전략 → TTL 기반으로 eventual consistency 허용, 상세 조회에만 캐시 적용
- 인덱스 설계 시 커버링 인덱스 vs 복합 인덱스 트레이드오프

---

### Week 6 - 결제 시스템 & Resilience

**구현 내용:**
- 주문/결제 상태 전이 모델 (State Machine 패턴)
- PG 연동 인터페이스 및 시뮬레이터 클라이언트
- 결제 오케스트레이션 및 보상 트랜잭션 (Saga 패턴)
- PG callback 상태 반영 흐름
- 결제 복구 스케줄러 (Pending 상태 타임아웃 처리)
- Resilience4j Bulkhead 적용
- 결제 동시성 통합 테스트

**고민했던 부분:**
- 결제 실패 시 보상 처리 방식 → Choreography vs Orchestration Saga → 결제 도메인이 중심이므로 Orchestration 방식 채택
- PG callback과 사용자 요청이 동시에 들어올 때의 상태 충돌 → 비관적 락으로 결제 상태 전이의 원자성 보장
- Bulkhead 설정값 (maxConcurrentCalls, maxWaitDuration) → 부하 테스트 기반으로 PG 응답 시간 고려해 설정

---

### Week 7 - 이벤트 기반 아키텍처 & 선착순 쿠폰

**구현 내용:**
- 주문 생성 이벤트 발행 (Spring ApplicationEvent + `@Async` 핸들러)
- Transactional Outbox 패턴 구현
  - OutboxEvent 엔티티 저장 → Scheduler가 Kafka로 relay → Consumer에서 멱등 처리
- Kafka Producer (acks=all, idempotence) / Consumer (manual ack)
- 선착순 쿠폰 발급 도메인 + Kafka 기반 비동기 처리
- Coupon 발급 요청 API 및 polling 조회
- OutboxEvent partitionKey 기반 Kafka partition 전략

**고민했던 부분:**
- 이벤트 발행과 DB 트랜잭션의 원자성 → `@TransactionalEventListener(AFTER_COMMIT)`만으로는 발행 실패 시 유실 가능 → Outbox 패턴으로 at-least-once 보장
- 멱등 처리를 어디서 할 것인가 → Consumer 측에 idempotency key 테이블을 두어 중복 소비 방어
- Kafka partition 전략 → 같은 주문의 이벤트는 순서 보장 필요 → orderId 기반 partitionKey 적용
- 선착순 쿠폰의 재고 관리 → Redis DECR로 원자적 차감 후 Kafka 이벤트로 실제 발급 처리

---

### Week 8 - 주문 대기열 시스템

**구현 내용:**
- Redis 기반 주문 대기열 (Sorted Set)
- 대기열 진입 API 및 토큰 검증 로직
- 예상 대기시간 계산 로직
- Lua Script로 dequeue + 토큰 발급 원자화
- 주문 성공 시 입장 토큰 삭제
- 동시성 테스트 (2000명 동시 진입, 처리량 초과, TTL 만료)

**고민했던 부분:**
- 대기열 dequeue와 토큰 발급을 어떻게 원자적으로 처리할 것인가 → 두 개의 Redis 명령을 개별 실행하면 중간에 장애 시 토큰 없는 유저가 빠져나갈 수 있음 → **Lua Script**로 ZPOPMIN + SET NX EX를 하나의 원자 연산으로 묶음
- 토큰 TTL 관리 → 너무 짧으면 정상 사용자도 만료, 너무 길면 좀비 토큰 누적 → 주문 완료 시 명시적 삭제 + TTL fallback 이중 전략
- 대기열 순서 보장 → Redis Sorted Set의 score를 timestamp로 사용하여 FIFO 보장

---

### Week 9 - 실시간 상품 랭킹 파이프라인

**구현 내용:**
- Ranking Score Policy 구현 (이벤트별 가중치 기반 점수 산출)
- Kafka Consumer → Redis ZSET 실시간 적재 파이프라인
- `ZUNIONSTORE` 기반 랭킹 carry-over (콜드 스타트 완화)
- 랭킹 carry-over Scheduler 구현 및 테스트
- 상품 랭킹 조회 API

**고민했던 부분:**
- 콜드 스타트 문제 → 자정에 새로운 날짜 키가 생성되면 랭킹이 비어있음 → `ZUNIONSTORE`로 전일 데이터를 가중치를 낮춰 carry-over, Scheduler가 자정에 자동 실행
- Redis ZSET 하나로 일별 랭킹을 관리하되, 키 네이밍 컨벤션(`ranking:all:{yyyyMMdd}`)으로 일자별 분리
- Kafka 배치 Consumer 설정 → `max.poll.records=3000`으로 처리량 확보, manual ack으로 유실 방지

---

### Week 10 - 배치 기반 랭킹 집계 & API 확장

**구현 내용:**
- **Daily Metrics Snapshot Batch**: Redis ZSET → MySQL `product_metrics` 테이블로 일별 스냅샷
  - Batch insert (1000건 단위 flush/clear) 로 메모리 효율 확보
  - Empty result guard (Redis 데이터 없을 시 기존 데이터 보호)
- **Weekly/Monthly Rank Aggregation Batch**: `product_metrics` 7일/30일 집계 → `mv_product_rank_weekly` / `mv_product_rank_monthly`
  - JdbcCursorItemReader로 대량 데이터 스트리밍 처리
  - Chunk 기반 처리 (chunk size: 100)
  - TOP 100 제한으로 불필요한 연산 방지
- **Ranking API 확장**: DAILY / WEEKLY / MONTHLY 기간별 랭킹 조회
- 모든 배치 작업 멱등성 보장 (동일 파라미터 재실행 시 동일 결과)

**고민했던 부분:**
- Redis 스냅샷을 왜 DB에 저장하는가 → Redis는 휘발성 + 장기 데이터 보관에 부적합 → MySQL에 일별 스냅샷을 남겨 주간/월간 집계의 안정적 원천 데이터 확보
- Batch insert 시 영속성 컨텍스트 관리 → 대량 insert 시 1차 캐시 메모리 폭증 → `entityManager.flush()` + `clear()`를 1000건마다 호출하여 메모리 제어
- Empty result guard → 배치 실행 시 Redis에 데이터가 없으면 기존 DB 데이터를 삭제하면 안됨 → Redis 결과가 비어있으면 스킵 처리
- 주간/월간 랭킹 테이블 설계 → View vs 물리 테이블 → 조회 성능과 집계 비용을 고려하여 물리 테이블(`mv_product_rank_weekly/monthly`)에 배치로 적재하는 Materialized View 전략 채택
- Writer에서의 delete 타이밍 → 첫 번째 chunk 처리 시에만 기존 데이터 삭제 (AtomicBoolean으로 제어), 이후 chunk는 insert만 수행

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
| 이벤트 발행 | Transactional Outbox 패턴 | DB 트랜잭션과 메시지 발행의 원자성 보장 |
| 대기열 원자성 | Redis Lua Script | dequeue + 토큰 발급을 단일 원자 연산으로 |
| 랭킹 콜드 스타트 | ZUNIONSTORE carry-over | 자정 랭킹 초기화 문제 해결 |
| 장기 랭킹 | Materialized View 전략 | Redis 휘발성 극복, 주간/월간 안정적 집계 |
| 배치 메모리 | flush/clear per 1000건 | 대량 insert 시 영속성 컨텍스트 OOM 방지 |
| 결제 보상 | Orchestration Saga | 결제 중심의 명확한 보상 흐름 |
| 캐시 전략 | TTL 기반 eventual consistency | 상품 상세 읽기 부하 분산 |
| Kafka 신뢰성 | acks=all + manual ack + 멱등 | 메시지 유실 방지 + 중복 소비 방어 |
| Redis 가용성 | Master-Replica + ReadFrom 분리 | 읽기 부하 분산, 장애 시 replica fallback |

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
