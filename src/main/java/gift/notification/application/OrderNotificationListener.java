package gift.notification.application;

import gift.notification.infrastructure.KakaoMessageClient;
import gift.order.application.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderNotificationListener {
    private final KakaoMessageClient kakaoMessageClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendGiftMessage(OrderCreatedEvent event) {
        if (event.kakaoAccessToken() == null) {
            return;
        }

        try {
            kakaoMessageClient.sendToMe(event.kakaoAccessToken(), new GiftMessage(
                event.productName(),
                event.optionName(),
                event.quantity(),
                event.totalPrice(),
                event.message()
            ));
        } catch (Exception ignored) {
        }
    }
}
