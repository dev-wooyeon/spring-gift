package gift.notification.application;

import gift.notification.infrastructure.KakaoMessageClient;
import gift.order.application.OrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderNotificationListenerTest {
    @Mock
    private KakaoMessageClient kakaoMessageClient;

    @Mock
    private NotificationMemberPort notificationMemberPort;

    @InjectMocks
    private OrderNotificationListener orderNotificationListener;

    @Test
    @DisplayName("주문 생성 이벤트의 memberId에 연결된 카카오 접근 토큰이 있으면 선물 메시지를 전송한다")
    void sendGiftMessageSendsMessageWhenAccessTokenExists() {
        // given
        OrderCreatedEvent event = event(1L);
        when(notificationMemberPort.getKakaoAccessToken(1L)).thenReturn(Optional.of("kakao-token"));

        // when
        orderNotificationListener.sendGiftMessage(event);

        // then
        ArgumentCaptor<GiftMessage> messageCaptor = ArgumentCaptor.forClass(GiftMessage.class);
        verify(kakaoMessageClient).sendToMe(eq("kakao-token"), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).isEqualTo(new GiftMessage(
            "키보드",
            "블랙",
            2,
            100_000,
            "축하합니다"
        ));
    }

    @Test
    @DisplayName("주문 생성 이벤트의 memberId에 연결된 카카오 접근 토큰이 없으면 선물 메시지를 전송하지 않는다")
    void sendGiftMessageSkipsMessageWhenAccessTokenIsNull() {
        // given
        OrderCreatedEvent event = event(1L);
        when(notificationMemberPort.getKakaoAccessToken(1L)).thenReturn(Optional.empty());

        // when
        orderNotificationListener.sendGiftMessage(event);

        // then
        verify(kakaoMessageClient, never()).sendToMe(anyString(), any());
    }

    @Test
    @DisplayName("카카오 메시지 전송 실패는 주문 흐름에 예외로 전파하지 않는다")
    void sendGiftMessageIgnoresKakaoClientException() {
        // given
        OrderCreatedEvent event = event(1L);
        when(notificationMemberPort.getKakaoAccessToken(1L)).thenReturn(Optional.of("kakao-token"));
        doThrow(new RuntimeException("카카오 메시지 전송 실패"))
            .when(kakaoMessageClient)
            .sendToMe(anyString(), any(GiftMessage.class));

        // when & then
        assertThatCode(() -> orderNotificationListener.sendGiftMessage(event))
            .doesNotThrowAnyException();
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
}
