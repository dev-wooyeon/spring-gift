package gift.wish.application;

public record WishView(
    Long id,
    Long productId,
    String name,
    int price,
    String imageUrl
) {
    public static WishView of(Long wishId, WishProduct product) {
        return new WishView(
            wishId,
            product.id(),
            product.name(),
            product.price(),
            product.imageUrl()
        );
    }
}
