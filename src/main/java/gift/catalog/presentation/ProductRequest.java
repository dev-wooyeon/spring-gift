package gift.catalog.presentation;

import gift.catalog.application.ProductCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductRequest(
    @NotBlank String name,
    @Positive int price,
    @NotBlank String imageUrl,
    @NotNull Long categoryId
) {
    public ProductCommand toCommand() {
        return new ProductCommand(name, price, imageUrl, categoryId);
    }
}
