package gift.order;

import gift.auth.AuthenticationResolver;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final AuthenticationResolver authenticationResolver;
    private final KakaoMessageClient kakaoMessageClient;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        MemberRepository memberRepository,
        AuthenticationResolver authenticationResolver,
        KakaoMessageClient kakaoMessageClient
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.authenticationResolver = authenticationResolver;
        this.kakaoMessageClient = kakaoMessageClient;
    }

    public Result<Page<OrderResponse>> getOrders(String authorization, Pageable pageable) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return Result.unauthorized();
        }

        Page<OrderResponse> orders = orderRepository.findByMemberId(member.getId(), pageable).map(OrderResponse::from);
        return Result.ok(orders);
    }

    public Result<OrderResponse> createOrder(String authorization, OrderRequest request) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return Result.unauthorized();
        }

        Option option = optionRepository.findById(request.optionId()).orElse(null);
        if (option == null) {
            return Result.notFound();
        }

        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        int price = option.getProduct().getPrice() * request.quantity();
        member.deductPoint(price);
        memberRepository.save(member);

        Order saved = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));
        sendKakaoMessageIfPossible(member, saved, option);
        return Result.created(OrderResponse.from(saved));
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

    public enum Status {
        OK,
        CREATED,
        UNAUTHORIZED,
        NOT_FOUND
    }

    public record Result<T>(Status status, T body) {
        static <T> Result<T> ok(T body) {
            return new Result<>(Status.OK, body);
        }

        static <T> Result<T> created(T body) {
            return new Result<>(Status.CREATED, body);
        }

        static <T> Result<T> unauthorized() {
            return new Result<>(Status.UNAUTHORIZED, null);
        }

        static <T> Result<T> notFound() {
            return new Result<>(Status.NOT_FOUND, null);
        }
    }
}
