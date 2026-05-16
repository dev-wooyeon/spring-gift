package gift.wish;

import gift.auth.AuthenticationResolver;
import gift.member.Member;
import gift.product.Product;
import gift.product.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WishService {
    private final WishRepository wishRepository;
    private final ProductService productService;
    private final AuthenticationResolver authenticationResolver;

    public WishService(
        WishRepository wishRepository,
        ProductService productService,
        AuthenticationResolver authenticationResolver
    ) {
        this.wishRepository = wishRepository;
        this.productService = productService;
        this.authenticationResolver = authenticationResolver;
    }

    public Result<Page<WishResponse>> getWishes(String authorization, Pageable pageable) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return Result.unauthorized();
        }

        Page<WishResponse> wishes = wishRepository.findByMemberId(member.getId(), pageable).map(WishResponse::from);
        return Result.ok(wishes);
    }

    @Transactional
    public Result<WishResponse> addWish(String authorization, WishRequest request) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return Result.unauthorized();
        }

        Product product = productService.findProduct(request.productId()).orElse(null);
        if (product == null) {
            return Result.notFound();
        }

        Wish existing = wishRepository.findByMemberIdAndProductId(member.getId(), product.getId()).orElse(null);
        if (existing != null) {
            return Result.ok(WishResponse.from(existing));
        }

        Wish saved = wishRepository.save(new Wish(member.getId(), product));
        return Result.created(WishResponse.from(saved));
    }

    @Transactional
    public Result<Void> removeWish(String authorization, Long id) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return Result.unauthorized();
        }

        Wish wish = wishRepository.findById(id).orElse(null);
        if (wish == null) {
            return Result.notFound();
        }

        if (!wish.getMemberId().equals(member.getId())) {
            return Result.forbidden();
        }

        wishRepository.delete(wish);
        return Result.noContent();
    }

    public enum Status {
        OK,
        CREATED,
        NO_CONTENT,
        UNAUTHORIZED,
        NOT_FOUND,
        FORBIDDEN
    }

    public record Result<T>(Status status, T body) {
        static <T> Result<T> ok(T body) {
            return new Result<>(Status.OK, body);
        }

        static <T> Result<T> created(T body) {
            return new Result<>(Status.CREATED, body);
        }

        static <T> Result<T> noContent() {
            return new Result<>(Status.NO_CONTENT, null);
        }

        static <T> Result<T> unauthorized() {
            return new Result<>(Status.UNAUTHORIZED, null);
        }

        static <T> Result<T> notFound() {
            return new Result<>(Status.NOT_FOUND, null);
        }

        static <T> Result<T> forbidden() {
            return new Result<>(Status.FORBIDDEN, null);
        }
    }
}
