package gift.catalog.presentation;

import gift.catalog.application.CategoryCommand;
import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
    @NotBlank String name,
    @NotBlank String color,
    @NotBlank String imageUrl,
    String description
) {
    public CategoryCommand toCommand() {
        return new CategoryCommand(name, color, imageUrl, description);
    }
}
