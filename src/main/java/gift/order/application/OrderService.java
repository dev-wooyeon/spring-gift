package gift.order.application;

import gift.order.domain.Order;
import gift.order.exception.OrderException;
import gift.order.infrastructure.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderOptionPort optionPort;
    private final OrderMemberPort memberPort;
    private final ApplicationEventPublisher eventPublisher;

    public Page<Order> getOrders(Long memberId, Pageable pageable) {
        log.info("[OrderService] 주문 목록 조회 - 회원 ID: {}", memberId);
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(OrderMember member, OrderCommand command) {
        log.info("[OrderService] 주문 생성 개시 - 회원 ID: {}, 옵션 ID: {}, 수량: {}",
            member.id(), command.optionId(), command.quantity());

        ReservedOption option = optionPort.reserveOption(command.optionId(), command.quantity())
            .orElseThrow(() -> {
                log.error("[OrderService] 주문 생성 실패 - 옵션이 존재하지 않음. 옵션 ID: {}", command.optionId());
                return OrderException.notFound("주문 옵션을 찾을 수 없습니다. id=" + command.optionId());
            });

        int price = option.totalPrice(command.quantity());
        log.info("[OrderService] 옵션 재고 확보 성공 - 상품명: {}, 옵션명: {}, 총 금액: {}",
            option.productName(), option.optionName(), price);

        memberPort.deductPoint(member.id(), price);
        log.info("[OrderService] 포인트 차감 성공 - 회원 ID: {}, 차감 금액: {}", member.id(), price);

        Order saved = orderRepository.save(new Order(option.optionId(), member.id(), command.quantity(), command.message()));
        log.info("[OrderService] 주문서 저장 성공 - 주문 ID: {}", saved.getId());

        eventPublisher.publishEvent(new OrderCreatedEvent(
            member.kakaoAccessToken(),
            option.productName(),
            option.optionName(),
            command.quantity(),
            price,
            command.message()
        ));
        log.info("[OrderService] 주문 생성 이벤트 발행 성공 - 주문 ID: {}", saved.getId());

        return saved;
    }
}
