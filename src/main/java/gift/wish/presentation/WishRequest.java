package gift.wish.presentation;

import gift.wish.application.WishCommand;
import jakarta.validation.constraints.NotNull;

public record WishRequest(@NotNull Long productId) {
    public WishCommand toCommand() {
        return new WishCommand(productId);
    }
}
