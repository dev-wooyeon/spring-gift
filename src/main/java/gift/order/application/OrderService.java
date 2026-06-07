package gift.order.application;

import gift.order.domain.Order;
import gift.order.infrastructure.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderOptionPort optionPort;
    private final OrderMemberPort memberPort;
    private final ApplicationEventPublisher eventPublisher;

    public Page<Order> getOrders(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional(rollbackFor = Exception.class)
    public CreateResult createOrder(Long memberId, OrderCommand command) {
        ReservedOption option = optionPort.reserveOption(command.optionId(), command.quantity()).orElse(null);
        if (option == null) {
            return CreateResult.optionMissing();
        }

        int price = option.totalPrice(command.quantity());
        memberPort.deductPoint(memberId, price);

        Order saved = orderRepository.save(new Order(option.optionId(), memberId, command.quantity(), command.message()));
        eventPublisher.publishEvent(new OrderCreatedEvent(
            memberId,
            option.productName(),
            option.optionName(),
            command.quantity(),
            price,
            command.message()
        ));
        return CreateResult.created(saved);
    }

    public enum CreateStatus {
        CREATED,
        OPTION_MISSING
    }

    public record CreateResult(CreateStatus status, Order order) {
        static CreateResult created(Order order) {
            return new CreateResult(CreateStatus.CREATED, order);
        }

        static CreateResult optionMissing() {
            return new CreateResult(CreateStatus.OPTION_MISSING, null);
        }
    }
}
