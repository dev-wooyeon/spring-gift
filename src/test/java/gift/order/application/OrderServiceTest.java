package gift.order.application;

import gift.order.domain.Order;
import gift.order.exception.OrderException;
import gift.order.infrastructure.OrderRepository;
import gift.point.exception.PointException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderOptionPort optionPort;

    @Mock
    private OrderMemberPort memberPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("회원 ID로 주문 목록을 조회한다")
    void getOrdersDelegatesToRepository() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);
        Order order = new Order(100L, 10L, 2, "메시지");
        when(orderRepository.findByMemberId(10L, pageable)).thenReturn(new PageImpl<>(List.of(order)));

        // when
        Page<Order> result = orderService.getOrders(10L, pageable);

        // then
        assertThat(result.getContent()).containsExactly(order);
    }

    @Test
    @DisplayName("옵션이 없으면 포인트 차감, 주문 저장, 이벤트 발행 없이 OrderException 예외가 발생한다")
    void createOrderThrowsOrderExceptionWhenOptionDoesNotExist() {
        // given
        OrderMember member = new OrderMember(10L, "kakao-token");
        OrderCommand command = new OrderCommand(100L, 2, "메시지");
        when(optionPort.reserveOption(100L, 2)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(member, command))
            .isInstanceOf(OrderException.class)
            .hasMessage("주문 옵션을 찾을 수 없습니다. id=100");
        verify(memberPort, never()).deductPoint(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("옵션 예약에 성공하면 총 주문 금액만큼 포인트를 차감하고 주문을 저장한 뒤 이벤트를 발행한다")
    void createOrderDeductsPointSavesOrderAndPublishesEvent() {
        // given
        OrderMember member = new OrderMember(10L, "kakao-token");
        OrderCommand command = new OrderCommand(100L, 3, "선물 메시지");
        ReservedOption option = new ReservedOption(100L, "블랙", 200L, "키보드", 50_000);
        when(optionPort.reserveOption(100L, 3)).thenReturn(Optional.of(option));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        // when
        Order result = orderService.createOrder(member, command);

        // then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(memberPort).deductPoint(10L, 150_000);
        verify(orderRepository).save(orderCaptor.capture());
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getOptionId()).isEqualTo(100L);
        assertThat(savedOrder.getMemberId()).isEqualTo(10L);
        assertThat(savedOrder.getQuantity()).isEqualTo(3);
        assertThat(savedOrder.getMessage()).isEqualTo("선물 메시지");
        assertThat(result.getId()).isEqualTo(1L);

        OrderCreatedEvent event = eventCaptor.getValue();
        assertThat(event.kakaoAccessToken()).isEqualTo("kakao-token");
        assertThat(event.productName()).isEqualTo("키보드");
        assertThat(event.optionName()).isEqualTo("블랙");
        assertThat(event.quantity()).isEqualTo(3);
        assertThat(event.totalPrice()).isEqualTo(150_000);
        assertThat(event.message()).isEqualTo("선물 메시지");
    }

    @Test
    @DisplayName("포인트 차감 중 예외가 발생하면 주문을 저장하지 않고 이벤트도 발행하지 않는다")
    void createOrderDoesNotSaveOrderWhenPointDeductionFails() {
        // given
        OrderMember member = new OrderMember(10L, "kakao-token");
        OrderCommand command = new OrderCommand(100L, 3, "선물 메시지");
        ReservedOption option = new ReservedOption(100L, "블랙", 200L, "키보드", 50_000);
        when(optionPort.reserveOption(100L, 3)).thenReturn(Optional.of(option));
        doThrow(PointException.invalid("회원 포인트가 부족합니다."))
            .when(memberPort)
            .deductPoint(10L, 150_000);

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(member, command))
            .isInstanceOf(PointException.class)
            .hasMessage("회원 포인트가 부족합니다.");
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("주문 저장 중 예외가 발생하면 이벤트를 발행하지 않는다")
    void createOrderDoesNotPublishEventWhenOrderSaveFails() {
        // given
        OrderMember member = new OrderMember(10L, "kakao-token");
        OrderCommand command = new OrderCommand(100L, 3, "선물 메시지");
        ReservedOption option = new ReservedOption(100L, "블랙", 200L, "키보드", 50_000);
        when(optionPort.reserveOption(100L, 3)).thenReturn(Optional.of(option));
        when(orderRepository.save(any(Order.class))).thenThrow(new IllegalStateException("저장 실패"));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(member, command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("저장 실패");
        verify(memberPort).deductPoint(10L, 150_000);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
