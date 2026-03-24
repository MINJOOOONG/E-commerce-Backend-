package com.loopers.domain.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    @DisplayName("OutboxEvent를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("eventType과 payload를 전달하면, 상태가 INIT으로 생성된다.")
        @Test
        void createsWithInitStatus() {
            // act
            OutboxEvent event = new OutboxEvent("OrderCreatedEvent", "{\"orderId\":1}");

            // assert
            assertThat(event.getEventType()).isEqualTo("OrderCreatedEvent");
            assertThat(event.getPayload()).isEqualTo("{\"orderId\":1}");
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.INIT);
        }
    }

    @DisplayName("markSent를 호출하면,")
    @Nested
    class MarkSent {

        @DisplayName("상태가 SENT로 변경된다.")
        @Test
        void changesStatusToSent() {
            // arrange
            OutboxEvent event = new OutboxEvent("OrderCreatedEvent", "{\"orderId\":1}");

            // act
            event.markSent();

            // assert
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        }
    }
}
