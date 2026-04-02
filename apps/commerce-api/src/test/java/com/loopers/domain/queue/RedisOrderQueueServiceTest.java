package com.loopers.domain.queue;

import com.loopers.testcontainers.RedisTestContainersConfig;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(RedisTestContainersConfig.class)
@DisplayName("RedisOrderQueueService 통합 테스트")
class RedisOrderQueueServiceTest {

    @Autowired
    private OrderQueueService orderQueueService;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    @DisplayName("대기열 진입 시, ")
    @Nested
    class Enqueue {

        @DisplayName("사용자가 대기열에 성공적으로 진입한다.")
        @Test
        void enqueue_success() {
            // arrange
            Long userId = 1L;

            // act
            boolean result = orderQueueService.enqueue(userId);

            // assert
            assertThat(result).isTrue();
            assertThat(orderQueueService.getPosition(userId)).isPresent();
        }

        @DisplayName("같은 사용자가 중복 진입하면 false를 반환한다.")
        @Test
        void enqueue_duplicate_returnsFalse() {
            // arrange
            Long userId = 1L;
            orderQueueService.enqueue(userId);

            // act
            boolean result = orderQueueService.enqueue(userId);

            // assert
            assertThat(result).isFalse();
        }

        @DisplayName("먼저 진입한 사용자가 앞 순번을 가진다.")
        @Test
        void enqueue_orderPreserved() throws InterruptedException {
            // arrange & act
            orderQueueService.enqueue(1L);
            Thread.sleep(1);
            orderQueueService.enqueue(2L);
            Thread.sleep(1);
            orderQueueService.enqueue(3L);

            // assert
            assertThat(orderQueueService.getPosition(1L)).contains(1L);
            assertThat(orderQueueService.getPosition(2L)).contains(2L);
            assertThat(orderQueueService.getPosition(3L)).contains(3L);
        }
    }

    @DisplayName("순번 조회 시, ")
    @Nested
    class GetPosition {

        @DisplayName("대기열에 없는 사용자는 빈 값을 반환한다.")
        @Test
        void getPosition_notInQueue_returnsEmpty() {
            // act
            Optional<Long> position = orderQueueService.getPosition(999L);

            // assert
            assertThat(position).isEmpty();
        }

        @DisplayName("전체 대기 인원을 조회할 수 있다.")
        @Test
        void getWaitingCount() {
            // arrange
            orderQueueService.enqueue(1L);
            orderQueueService.enqueue(2L);
            orderQueueService.enqueue(3L);

            // act
            long count = orderQueueService.getWaitingCount();

            // assert
            assertThat(count).isEqualTo(3);
        }
    }

    @DisplayName("대기열에서 꺼낼 때, ")
    @Nested
    class Dequeue {

        @DisplayName("앞에서부터 N명을 꺼낸다.")
        @Test
        void dequeue_popsFromFront() throws InterruptedException {
            // arrange
            orderQueueService.enqueue(1L);
            Thread.sleep(1);
            orderQueueService.enqueue(2L);
            Thread.sleep(1);
            orderQueueService.enqueue(3L);

            // act
            List<Long> dequeued = orderQueueService.dequeue(2);

            // assert
            assertThat(dequeued).containsExactly(1L, 2L);
            assertThat(orderQueueService.getWaitingCount()).isEqualTo(1);
        }

        @DisplayName("대기열이 비어있으면 빈 리스트를 반환한다.")
        @Test
        void dequeue_emptyQueue_returnsEmptyList() {
            // act
            List<Long> dequeued = orderQueueService.dequeue(5);

            // assert
            assertThat(dequeued).isEmpty();
        }
    }

    @DisplayName("입장 토큰 관리 시, ")
    @Nested
    class Token {

        @DisplayName("토큰을 발급하고 조회할 수 있다.")
        @Test
        void issueAndGetToken() {
            // arrange
            Long userId = 1L;
            String token = "test-token-uuid";

            // act
            orderQueueService.issueToken(userId, token, 300L);
            Optional<String> result = orderQueueService.getToken(userId);

            // assert
            assertThat(result).contains(token);
        }

        @DisplayName("토큰을 삭제할 수 있다.")
        @Test
        void removeToken() {
            // arrange
            Long userId = 1L;
            orderQueueService.issueToken(userId, "token", 300L);

            // act
            orderQueueService.removeToken(userId);

            // assert
            assertThat(orderQueueService.getToken(userId)).isEmpty();
        }

        @DisplayName("토큰이 없는 사용자는 빈 값을 반환한다.")
        @Test
        void getToken_noToken_returnsEmpty() {
            // act
            Optional<String> result = orderQueueService.getToken(999L);

            // assert
            assertThat(result).isEmpty();
        }
    }
}
