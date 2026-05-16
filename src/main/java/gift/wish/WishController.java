package gift.wish;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/wishes")
public class WishController {
    private final WishService wishService;

    public WishController(WishService wishService) {
        this.wishService = wishService;
    }

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        WishService.Result<Page<WishResponse>> result = wishService.getWishes(authorization, pageable);
        if (result.status() == WishService.Status.UNAUTHORIZED) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(result.body());
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody WishRequest request
    ) {
        WishService.Result<WishResponse> result = wishService.addWish(authorization, request);
        if (result.status() == WishService.Status.UNAUTHORIZED) {
            return ResponseEntity.status(401).build();
        }
        if (result.status() == WishService.Status.NOT_FOUND) {
            return ResponseEntity.notFound().build();
        }
        if (result.status() == WishService.Status.OK) {
            return ResponseEntity.ok(result.body());
        }

        WishResponse response = result.body();
        return ResponseEntity.created(URI.create("/api/wishes/" + response.id()))
            .body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @RequestHeader("Authorization") String authorization,
        @PathVariable Long id
    ) {
        WishService.Result<Void> result = wishService.removeWish(authorization, id);
        if (result.status() == WishService.Status.UNAUTHORIZED) {
            return ResponseEntity.status(401).build();
        }
        if (result.status() == WishService.Status.NOT_FOUND) {
            return ResponseEntity.notFound().build();
        }
        if (result.status() == WishService.Status.FORBIDDEN) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.noContent().build();
    }
}
