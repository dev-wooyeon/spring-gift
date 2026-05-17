package gift.wish;

import gift.member.Member;
import gift.product.Product;
import gift.product.ProductService;
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
    private final ProductService productService;

    public Page<Wish> getWishes(Member member, Pageable pageable) {
        return wishRepository.findByMemberId(member.getId(), pageable);
    }

    @Transactional
    public CreateResult addWish(Member member, WishRequest request) {
        Product product = productService.findProduct(request.productId()).orElse(null);
        if (product == null) {
            return CreateResult.productMissing();
        }

        Wish existing = wishRepository.findByMemberIdAndProductId(member.getId(), product.getId()).orElse(null);
        if (existing != null) {
            return CreateResult.existing(existing);
        }

        Wish saved = wishRepository.save(new Wish(member.getId(), product));
        return CreateResult.created(saved);
    }

    @Transactional
    public DeleteResult removeWish(Member member, Long id) {
        Wish wish = wishRepository.findById(id).orElse(null);
        if (wish == null) {
            return DeleteResult.notFound();
        }

        if (!wish.getMemberId().equals(member.getId())) {
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

    public record CreateResult(CreateStatus status, Wish wish) {
        static CreateResult created(Wish wish) {
            return new CreateResult(CreateStatus.CREATED, wish);
        }

        static CreateResult existing(Wish wish) {
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
