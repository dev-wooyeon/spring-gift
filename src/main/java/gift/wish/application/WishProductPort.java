package gift.wish.application;

import java.util.Optional;

public interface WishProductPort {
    Optional<WishProduct> findProduct(Long productId);
}
