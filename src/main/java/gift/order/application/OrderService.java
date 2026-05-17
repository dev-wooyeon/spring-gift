package gift.order.application;

import gift.catalog.application.OptionService;
import gift.catalog.domain.Option;
import gift.member.application.MemberService;
import gift.member.domain.Member;
import gift.notification.application.NotificationService;
import gift.order.domain.Order;
import gift.order.infrastructure.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionService optionService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    public Page<Order> getOrders(Member member, Pageable pageable) {
        return orderRepository.findByMemberId(member.getId(), pageable);
    }

    public CreateResult createOrder(Member member, OrderCommand command) {
        Option option = optionService.reserveOption(command.optionId(), command.quantity()).orElse(null);
        if (option == null) {
            return CreateResult.optionMissing();
        }

        int price = option.getProduct().getPrice() * command.quantity();
        memberService.deductPoint(member, price);

        Order saved = orderRepository.save(new Order(option, member.getId(), command.quantity(), command.message()));
        notificationService.sendGiftMessage(member, saved);
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
