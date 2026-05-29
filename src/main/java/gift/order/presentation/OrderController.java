package gift.order.presentation;

import gift.auth.support.AuthenticationResolver;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final AuthenticationResolver authenticationResolver;

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(orderService.getOrders(member.getId(), pageable).map(OrderResponse::from));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody OrderRequest request
    ) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        Order order = orderService.createOrder(
            new OrderMember(member.getId(), member.getKakaoAccessToken()),
            request.toCommand()
        );

        OrderResponse response = OrderResponse.from(order);
        return ResponseEntity.created(URI.create("/api/orders/" + response.id()))
            .body(response);
    }
}
