package gift.wish.application;

import gift.wish.domain.Wish;
import gift.wish.infrastructure.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WishService {
    private final WishRepository wishRepository;
    private final WishProductPort productPort;

    public Page<WishView> getWishes(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable)
            .map(this::toView);
    }

    @Transactional
    public CreateResult addWish(Long memberId, WishCommand command) {
        WishProduct product = productPort.findProduct(command.productId()).orElse(null);
        if (product == null) {
            return CreateResult.productMissing();
        }

        Wish existing = wishRepository.findByMemberIdAndProductId(memberId, product.id()).orElse(null);
        if (existing != null) {
            return CreateResult.existing(WishView.of(existing.getId(), product));
        }

        Wish saved = wishRepository.save(new Wish(memberId, product.id()));
        return CreateResult.created(WishView.of(saved.getId(), product));
    }

    @Transactional
    public DeleteResult removeWish(Long memberId, Long id) {
        Wish wish = wishRepository.findById(id).orElse(null);
        if (wish == null) {
            return DeleteResult.notFound();
        }

        if (!wish.getMemberId().equals(memberId)) {
            return DeleteResult.notOwner();
        }

        wishRepository.delete(wish);
        return DeleteResult.deleted();
    }

    public enum CreateStatus {
        CREATED,
        EXISTING,
        PRODUCT_MISSING
    }

    private WishView toView(Wish wish) {
        WishProduct product = productPort.findProduct(wish.getProductId())
            .orElseThrow(() -> new IllegalStateException("상품이 존재하지 않습니다. id=" + wish.getProductId()));
        return WishView.of(wish.getId(), product);
    }

    public record CreateResult(CreateStatus status, WishView wish) {
        static CreateResult created(WishView wish) {
            return new CreateResult(CreateStatus.CREATED, wish);
        }

        static CreateResult existing(WishView wish) {
            return new CreateResult(CreateStatus.EXISTING, wish);
        }

        static CreateResult productMissing() {
            return new CreateResult(CreateStatus.PRODUCT_MISSING, null);
        }
    }

    public enum DeleteStatus {
        DELETED,
        WISH_MISSING,
        NOT_OWNER
    }

    public record DeleteResult(DeleteStatus status) {
        static DeleteResult deleted() {
            return new DeleteResult(DeleteStatus.DELETED);
        }

        static DeleteResult notFound() {
            return new DeleteResult(DeleteStatus.WISH_MISSING);
        }

        static DeleteResult notOwner() {
            return new DeleteResult(DeleteStatus.NOT_OWNER);
        }
    }
}
