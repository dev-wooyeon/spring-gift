package gift.catalog.application;

public record CategoryCommand(
    String name,
    String color,
    String imageUrl,
    String description
) {
}
