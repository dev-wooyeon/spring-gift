package gift.wish.application;

import gift.wish.domain.Wish;
import gift.wish.exception.WishException;
import gift.wish.infrastructure.WishRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WishService {
    private final WishRepository wishRepository;
    private final WishProductPort productPort;

    public Page<WishView> getWishes(Long memberId, Pageable pageable) {
        Page<Wish> wishes = wishRepository.findByMemberId(memberId, pageable);
        List<Long> productIds = wishes.stream()
            .map(Wish::getProductId)
            .distinct()
            .collect(Collectors.toList());

        List<WishProduct> products = productPort.findProducts(productIds);
        Map<Long, WishProduct> productMap = products.stream()
            .collect(Collectors.toMap(WishProduct::id, Function.identity()));

        return wishes.map(wish -> {
            WishProduct product = productMap.get(wish.getProductId());
            if (product == null) {
                log.error("Wish product missing. wishId={}, productId={}", wish.getId(), wish.getProductId());
                throw WishException.internal("위시에 연결된 상품을 찾을 수 없습니다.");
            }
            return WishView.of(wish.getId(), product);
        });
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
