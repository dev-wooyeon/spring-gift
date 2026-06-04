package gift.order.application;

import gift.notification.application.OrderNotificationListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
class OrderTransactionTest {

    @Autowired
    private OrderService orderService;

    @MockitoBean
    private OrderOptionPort optionPort;

    @MockitoBean
    private OrderMemberPort memberPort;

    @MockitoBean
    private OrderNotificationListener orderNotificationListener;

    @Test
    @DisplayName("Checked Exception 발생 시 트랜잭션이 롤백되고 @TransactionalEventListener가 실행되지 않는다")
    void transactionRollsBackOnCheckedExceptionAndEventListenerNotInvoked() throws Exception {
        // given
        OrderMember member = new OrderMember(1L, "kakao-token");
        OrderCommand command = new OrderCommand(100L, 1, "선물 메시지");
        ReservedOption option = new ReservedOption(100L, "블랙", 200L, "키보드", 50_000);

        when(optionPort.reserveOption(100L, 1)).thenReturn(Optional.of(option));

        // checked exception (e.g. IOException)이 발생하도록 설정
        doAnswer(invocation -> {
            throw new IOException("Checked Exception 발생");
        }).when(memberPort).deductPoint(anyLong(), anyInt());

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(member, command))
            .isInstanceOf(Exception.class)
            .hasRootCauseInstanceOf(IOException.class)
            .hasRootCauseMessage("Checked Exception 발생");

        // then: TransactionalEventListener는 phase=AFTER_COMMIT이므로, 롤백되면 실행되지 않아야 한다.
        verify(orderNotificationListener, never()).sendGiftMessage(any());
    }
}
