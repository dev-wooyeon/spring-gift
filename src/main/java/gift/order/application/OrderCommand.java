package gift.order.application;

public record OrderCommand(
    Long optionId,
    int quantity,
    String message
) {
}
