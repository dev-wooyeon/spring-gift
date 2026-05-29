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

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WishService {
    private final WishRepository wishRepository;
    private final WishProductPort productPort;

    public Page<WishView> getWishes(Long memberId, Pageable pageable) {
        log.info("[WishService] 위시리스트 목록 조회 - 회원 ID: {}", memberId);
        return wishRepository.findByMemberId(memberId, pageable)
            .map(this::toView);
    }

    @Transactional
    public CreateResult addWish(Long memberId, WishCommand command) {
        log.info("[WishService] 위시 등록 요청 - 회원 ID: {}, 상품 ID: {}", memberId, command.productId());
        WishProduct product = productPort.findProduct(command.productId()).orElse(null);
        if (product == null) {
            log.error("[WishService] 위시 등록 실패 - 존재하지 않는 상품 ID: {}", command.productId());
            return CreateResult.productMissing();
        }

        Wish existing = wishRepository.findByMemberIdAndProductId(memberId, product.id()).orElse(null);
        if (existing != null) {
            log.info("[WishService] 위시 등록 중단 - 이미 등록되어 있는 위시. 회원 ID: {}, 상품 ID: {}", memberId, product.id());
            return CreateResult.existing(WishView.of(existing.getId(), product));
        }

        Wish saved = wishRepository.save(new Wish(memberId, product.id()));
        log.info("[WishService] 위시 등록 성공 - 위시 ID: {}", saved.getId());
        return CreateResult.created(WishView.of(saved.getId(), product));
    }

    @Transactional
    public DeleteResult removeWish(Long memberId, Long id) {
        log.info("[WishService] 위시 삭제 요청 - 회원 ID: {}, 위시 ID: {}", memberId, id);
        Wish wish = wishRepository.findById(id).orElse(null);
        if (wish == null) {
            log.error("[WishService] 위시 삭제 실패 - 존재하지 않는 위시 ID: {}", id);
            return DeleteResult.notFound();
        }

        if (!wish.getMemberId().equals(memberId)) {
            log.error("[WishService] 위시 삭제 거부 - 권한 없는 위시 삭제 시도. 요청자 ID: {}, 위시 소유자 ID: {}",
                memberId, wish.getMemberId());
            return DeleteResult.notOwner();
        }

        wishRepository.delete(wish);
        log.info("[WishService] 위시 삭제 성공 - 위시 ID: {}", id);
        return DeleteResult.deleted();
    }

    public enum CreateStatus {
        CREATED,
        EXISTING,
        PRODUCT_MISSING
    }

    private WishView toView(Wish wish) {
        WishProduct product = productPort.findProduct(wish.getProductId())
            .orElseThrow(() -> {
                log.error("[WishService] 위시 뷰 조립 실패 - 상품 유실. 상품 ID: {}", wish.getProductId());
                return WishException.internal("위시에 연결된 상품을 찾을 수 없습니다. productId=" + wish.getProductId());
            });
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
