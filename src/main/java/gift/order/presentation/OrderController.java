package gift.order.presentation;

import gift.auth.presentation.LoginMember;
import gift.member.domain.Member;
import gift.order.application.OrderMember;
import gift.order.application.OrderService;
import gift.order.domain.Order;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Controller handling user orders.
 * Employs @LoginMember for seamless authentication mapping.
 *
 * @author brian.kim
 * @since 1.0
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
        @LoginMember Member member,
        Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.getOrders(member.getId(), pageable).map(OrderResponse::from));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @LoginMember Member member,
        @Valid @RequestBody OrderRequest request
    ) {
        Order order = orderService.createOrder(
            new OrderMember(member.getId(), member.getKakaoAccessToken()),
            request.toCommand()
        );

        OrderResponse response = OrderResponse.from(order);
        return ResponseEntity.created(URI.create("/api/orders/" + response.id()))
            .body(response);
    }
}
