package gift.order;

import jakarta.validation.Valid;
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
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        OrderService.Result<Page<OrderResponse>> result = orderService.getOrders(authorization, pageable);
        if (result.status() == OrderService.Status.UNAUTHORIZED) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(result.body());
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody OrderRequest request
    ) {
        OrderService.Result<OrderResponse> result = orderService.createOrder(authorization, request);
        if (result.status() == OrderService.Status.UNAUTHORIZED) {
            return ResponseEntity.status(401).build();
        }
        if (result.status() == OrderService.Status.NOT_FOUND) {
            return ResponseEntity.notFound().build();
        }

        OrderResponse response = result.body();
        return ResponseEntity.created(URI.create("/api/orders/" + response.id()))
            .body(response);
    }
}
