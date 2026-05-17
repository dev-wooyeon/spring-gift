package gift.notification.application;

public record GiftMessage(
    String productName,
    String optionName,
    int quantity,
    int totalPrice,
    String message
) {
}
