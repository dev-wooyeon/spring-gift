package gift.order.application;

public record OrderCreatedEvent(
    Long memberId,
    String productName,
    String optionName,
    int quantity,
    int totalPrice,
    String message
) {
}
