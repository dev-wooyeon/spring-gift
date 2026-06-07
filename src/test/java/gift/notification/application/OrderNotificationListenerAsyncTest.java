package gift.notification.application;

import gift.notification.infrastructure.KakaoMessageClient;
import gift.order.application.OrderCreatedEvent;
import gift.support.AsyncConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = {
    AsyncConfig.class,
    OrderNotificationListener.class,
    OrderNotificationListenerAsyncTest.TestConfig.class
})
class OrderNotificationListenerAsyncTest {
    private final KakaoMessageClient kakaoMessageClient;
    private final NotificationMemberPort notificationMemberPort;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    OrderNotificationListenerAsyncTest(
        KakaoMessageClient kakaoMessageClient,
        NotificationMemberPort notificationMemberPort,
        ApplicationEventPublisher eventPublisher,
        TransactionTemplate transactionTemplate
    ) {
        this.kakaoMessageClient = kakaoMessageClient;
        this.notificationMemberPort = notificationMemberPort;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    @Test
    @DisplayName("주문 알림 리스너는 커밋 이후 알림 전용 executor에서 비동기로 실행된다")
    void sendGiftMessageRunsAsynchronouslyAfterCommitOnNotificationExecutor() throws InterruptedException {
        // given
        CountDownLatch tokenLookupStarted = new CountDownLatch(1);
        AtomicReference<String> listenerThreadName = new AtomicReference<>();
        OrderCreatedEvent event = event(1L);

        when(notificationMemberPort.getKakaoAccessToken(1L)).thenAnswer(invocation -> {
            listenerThreadName.set(Thread.currentThread().getName());
            tokenLookupStarted.countDown();
            return Optional.of("kakao-token");
        });

        // when
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        // then
        assertThat(tokenLookupStarted.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(listenerThreadName.get()).startsWith("notification-");
        verify(kakaoMessageClient, timeout(1_000)).sendToMe(eq("kakao-token"), any(GiftMessage.class));
    }

    private OrderCreatedEvent event(Long memberId) {
        return new OrderCreatedEvent(
            memberId,
            "키보드",
            "블랙",
            2,
            100_000,
            "축하합니다"
        );
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {
        @Bean
        KakaoMessageClient kakaoMessageClient() {
            return mock(KakaoMessageClient.class);
        }

        @Bean
        NotificationMemberPort notificationMemberPort() {
            return mock(NotificationMemberPort.class);
        }

        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .build();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager);
        }
    }
}
