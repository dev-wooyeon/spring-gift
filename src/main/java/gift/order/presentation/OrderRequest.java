package gift.order.presentation;

import gift.order.application.OrderCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(
    @NotNull Long optionId,
    @Min(1) int quantity,
    String message
) {
    public OrderCommand toCommand() {
        return new OrderCommand(optionId, quantity, message);
    }
}
