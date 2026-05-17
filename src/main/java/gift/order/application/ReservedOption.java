package gift.order.application;

public record ReservedOption(
    Long optionId,
    String optionName,
    Long productId,
    String productName,
    int productPrice
) {
    public int totalPrice(int quantity) {
        return productPrice * quantity;
    }
}
