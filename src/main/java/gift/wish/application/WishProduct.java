package gift.wish.application;

public record WishProduct(
    Long id,
    String name,
    int price,
    String imageUrl
) {
}
