package gift.wish.presentation;

import gift.auth.presentation.LoginMember;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Controller handling user wishes.
 * Employs @LoginMember for seamless authentication mapping.
 *
 * @author brian.kim
 * @since 1.0
 */
@RestController
@RequestMapping("/api/wishes")
@RequiredArgsConstructor
public class WishController {
    private final WishService wishService;

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
        @LoginMember Member member,
        Pageable pageable
    ) {
        return ResponseEntity.ok(wishService.getWishes(member.getId(), pageable).map(WishResponse::from));
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @LoginMember Member member,
        @Valid @RequestBody WishRequest request
    ) {
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
        @LoginMember Member member,
        @PathVariable Long id
    ) {
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
