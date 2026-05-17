package gift.wish.presentation;

import gift.wish.application.WishView;

public record WishResponse(
    Long id,
    Long productId,
    String name,
    int price,
    String imageUrl
) {
    public static WishResponse from(WishView wish) {
        return new WishResponse(
            wish.id(),
            wish.productId(),
            wish.name(),
            wish.price(),
            wish.imageUrl()
        );
    }
}
