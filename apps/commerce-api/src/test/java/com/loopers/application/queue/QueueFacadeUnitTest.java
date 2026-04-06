package com.loopers.application.queue;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QueueFacade 단위 테스트")
class QueueFacadeUnitTest {

    private FakeOrderQueueService fakeQueueService;
    private QueueFacade queueFacade;

    @BeforeEach
    void setUp() {
        fakeQueueService = new FakeOrderQueueService();
        queueFacade = new QueueFacade(fakeQueueService);
    }

    @DisplayName("대기열 진입 시, ")
    @Nested
    class Enter {

        @DisplayName("사용자가 대기열에 진입하면 순번과 대기 인원을 반환한다.")
        @Test
        void enter_success() {
            // act
            QueueInfo.EnterResult result = queueFacade.enter(1L);

            // assert
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.position()).isEqualTo(1);
            assertThat(result.totalWaiting()).isEqualTo(1);
        }

        @DisplayName("이미 대기열에 있는 사용자가 진입하면 현재 순번을 반환한다.")
        @Test
        void enter_alreadyInQueue_returnsCurrentPosition() {
            // arrange
            queueFacade.enter(1L);

            // act
            QueueInfo.EnterResult result = queueFacade.enter(1L);

            // assert
            assertThat(result.position()).isEqualTo(1);
        }
    }

    @DisplayName("순번 조회 시, ")
    @Nested
    class GetPosition {

        @DisplayName("대기열에 있는 사용자의 순번, 대기 인원, 예상 대기 시간을 반환한다.")
        @Test
        void getPosition_inQueue() {
            // arrange
            fakeQueueService.enqueue(1L);
            fakeQueueService.enqueue(2L);
            fakeQueueService.enqueue(3L);

            // act
            QueueInfo.PositionResult result = queueFacade.getPosition(2L);

            // assert
            assertThat(result.userId()).isEqualTo(2L);
            assertThat(result.position()).isEqualTo(2);
            assertThat(result.totalWaiting()).isEqualTo(3);
            assertThat(result.estimatedWaitSeconds()).isGreaterThan(0);
            assertThat(result.token()).isNull();
        }

        @DisplayName("입장 토큰이 있는 사용자는 토큰을 포함하여 반환한다.")
        @Test
        void getPosition_withToken() {
            // arrange
            fakeQueueService.issueToken(1L, "my-token", 300L);

            // act
            QueueInfo.PositionResult result = queueFacade.getPosition(1L);

            // assert
            assertThat(result.position()).isEqualTo(0);
            assertThat(result.token()).isEqualTo("my-token");
        }

        @DisplayName("대기열에 없고 토큰도 없는 사용자는 예외를 던진다.")
        @Test
        void getPosition_notInQueue_throws() {
            // act & assert
            assertThatThrownBy(() -> queueFacade.getPosition(999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @DisplayName("대기열 처리 시, ")
    @Nested
    class ProcessQueue {

        @DisplayName("스케줄러가 배치 크기만큼 사용자를 꺼내 토큰을 발급한다.")
        @Test
        void processQueue_issuesTokens() {
            // arrange
            for (long i = 1; i <= 15; i++) {
                fakeQueueService.enqueue(i);
            }

            // act
            queueFacade.processQueue();

            // assert - BATCH_SIZE=10이므로 10명에게 토큰 발급
            for (long i = 1; i <= 10; i++) {
                assertThat(fakeQueueService.getToken(i)).isPresent();
            }
            for (long i = 11; i <= 15; i++) {
                assertThat(fakeQueueService.getToken(i)).isEmpty();
            }
            assertThat(fakeQueueService.getWaitingCount()).isEqualTo(5);
        }

        @DisplayName("BATCH_SIZE보다 많은 유저가 대기 중일 때, 1회 처리 후 초과분은 대기열에 남는다.")
        @Test
        void processQueue_exceedsBatchSize_remainsInQueue() {
            // arrange — 30명 진입 (BATCH_SIZE=10의 3배)
            int totalUsers = 30;
            for (long i = 1; i <= totalUsers; i++) {
                fakeQueueService.enqueue(i);
            }

            // act — processQueue 1회만 호출
            queueFacade.processQueue();

            // assert
            // 1) 토큰 발급된 유저 == BATCH_SIZE(10)명
            int tokenCount = 0;
            for (long i = 1; i <= totalUsers; i++) {
                if (fakeQueueService.getToken(i).isPresent()) {
                    tokenCount++;
                }
            }
            assertThat(tokenCount).isEqualTo(QueueFacade.BATCH_SIZE);

            // 2) 대기열에 남은 유저 == 30 - 10 = 20명
            assertThat(fakeQueueService.getWaitingCount()).isEqualTo(totalUsers - QueueFacade.BATCH_SIZE);

            // 3) 앞쪽 10명만 토큰 보유, 뒤쪽 20명은 토큰 없음
            for (long i = 1; i <= 10; i++) {
                assertThat(fakeQueueService.getToken(i)).isPresent();
            }
            for (long i = 11; i <= 30; i++) {
                assertThat(fakeQueueService.getToken(i)).isEmpty();
            }
        }

        @DisplayName("BATCH_SIZE보다 많은 유저가 있을 때, processQueue를 반복하면 순차적으로 전원 처리된다.")
        @Test
        void processQueue_multipleRounds_drainsAll() {
            // arrange — 25명 진입
            int totalUsers = 25;
            for (long i = 1; i <= totalUsers; i++) {
                fakeQueueService.enqueue(i);
            }

            // act — 3회 호출 (10 + 10 + 5)
            queueFacade.processQueue();
            queueFacade.processQueue();
            queueFacade.processQueue();

            // assert — 전원 토큰 발급, 대기열 비어 있음
            assertThat(fakeQueueService.getWaitingCount()).isEqualTo(0);
            for (long i = 1; i <= totalUsers; i++) {
                assertThat(fakeQueueService.getToken(i)).isPresent();
            }
        }
    }

    @DisplayName("토큰 검증 시, ")
    @Nested
    class ValidateToken {

        @DisplayName("유효한 토큰이 있으면 검증 성공 후 토큰을 삭제한다.")
        @Test
        void validateAndConsumeToken_success() {
            // arrange
            fakeQueueService.issueToken(1L, "valid-token", 300L);

            // act
            queueFacade.validateAndConsumeToken(1L);

            // assert
            assertThat(fakeQueueService.getToken(1L)).isEmpty();
        }

        @DisplayName("토큰이 없으면 예외를 던진다.")
        @Test
        void validateAndConsumeToken_noToken_throws() {
            // act & assert
            assertThatThrownBy(() -> queueFacade.validateAndConsumeToken(1L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.FORBIDDEN));
        }
    }
}
