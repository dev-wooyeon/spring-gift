package gift.catalog.application;

public record ProductCommand(
    String name,
    int price,
    String imageUrl,
    Long categoryId
) {
}
