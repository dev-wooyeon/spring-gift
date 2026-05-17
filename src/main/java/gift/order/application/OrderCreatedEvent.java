package gift.order.application;

public record OrderCreatedEvent(
    String kakaoAccessToken,
    String productName,
    String optionName,
    int quantity,
    int totalPrice,
    String message
) {
}
