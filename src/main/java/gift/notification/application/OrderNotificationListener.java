package gift.notification.application;

import gift.notification.infrastructure.KakaoMessageClient;
import gift.order.application.OrderCreatedEvent;
import gift.support.AsyncConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {
    private final KakaoMessageClient kakaoMessageClient;
    private final NotificationMemberPort notificationMemberPort;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendGiftMessage(OrderCreatedEvent event) {
        try {
            String token = notificationMemberPort.getKakaoAccessToken(event.memberId()).orElse(null);
            if (token == null) {
                return;
            }

            kakaoMessageClient.sendToMe(token, new GiftMessage(
                event.productName(),
                event.optionName(),
                event.quantity(),
                event.totalPrice(),
                event.message()
            ));
        } catch (Exception e) {
            log.error("Failed to send Kakao gift notification for memberId={}", event.memberId(), e);
        }
    }
}
