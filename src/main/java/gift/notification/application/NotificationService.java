package gift.notification.application;

import gift.member.domain.Member;
import gift.notification.infrastructure.KakaoMessageClient;
import gift.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final KakaoMessageClient kakaoMessageClient;

    public void sendGiftMessage(Member member, Order order) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }

        try {
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, order.getOption().getProduct());
        } catch (Exception ignored) {
        }
    }
}
