package gift.wish.presentation;

import gift.auth.support.AuthenticationResolver;
import gift.member.domain.Member;
import gift.wish.application.WishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class WishController {
    private final WishService wishService;
    private final AuthenticationResolver authenticationResolver;

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(wishService.getWishes(member.getId(), pageable).map(WishResponse::from));
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody WishRequest request
    ) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        WishService.CreateResult result = wishService.addWish(member.getId(), request.toCommand());
        if (result.status() == WishService.CreateStatus.PRODUCT_MISSING) {
            return ResponseEntity.notFound().build();
        }
        if (result.status() == WishService.CreateStatus.EXISTING) {
            return ResponseEntity.ok(WishResponse.from(result.wish()));
        }

        WishResponse response = WishResponse.from(result.wish());
        return ResponseEntity.created(URI.create("/api/wishes/" + response.id()))
            .body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @RequestHeader("Authorization") String authorization,
        @PathVariable Long id
    ) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        WishService.DeleteResult result = wishService.removeWish(member.getId(), id);
        if (result.status() == WishService.DeleteStatus.WISH_MISSING) {
            return ResponseEntity.notFound().build();
        }
        if (result.status() == WishService.DeleteStatus.NOT_OWNER) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.noContent().build();
    }
}
