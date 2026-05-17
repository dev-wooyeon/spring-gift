package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final KakaoMessageClient kakaoMessageClient;

    public Page<Order> getOrders(Member member, Pageable pageable) {
        return orderRepository.findByMemberId(member.getId(), pageable);
    }

    public CreateResult createOrder(Member member, OrderRequest request) {
        Option option = optionRepository.findById(request.optionId()).orElse(null);
        if (option == null) {
            return CreateResult.optionMissing();
        }

        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        int price = option.getProduct().getPrice() * request.quantity();
        member.deductPoint(price);
        memberRepository.save(member);

        Order saved = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));
        sendKakaoMessageIfPossible(member, saved, option);
        return CreateResult.created(saved);
    }

    private void sendKakaoMessageIfPossible(Member member, Order order, Option option) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            var product = option.getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception ignored) {
        }
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
