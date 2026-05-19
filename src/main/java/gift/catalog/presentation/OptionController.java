package gift.catalog.presentation;

import gift.catalog.application.OptionService;
import gift.catalog.domain.Option;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/*
 * Each product must have at least one option at all times.
 * Option names are validated against allowed characters and length constraints.
 */
@RestController
@RequestMapping(path = "/api/products/{productId}/options")
@RequiredArgsConstructor
public class OptionController {
    private final OptionService optionService;

    @GetMapping
    public ResponseEntity<List<OptionResponse>> getOptions(@PathVariable Long productId) {
        Optional<List<Option>> options = optionService.getOptions(productId);
        if (options.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<OptionResponse> response = options.get().stream()
            .map(OptionResponse::from)
            .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<OptionResponse> createOption(
        @PathVariable Long productId,
        @Valid @RequestBody OptionRequest request
    ) {
        Optional<Option> saved = optionService.createOption(productId, request.toCommand());
        if (saved.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        OptionResponse response = OptionResponse.from(saved.get());
        URI location = URI.create("/api/products/" + productId + "/options/" + response.id());
        return ResponseEntity.created(location)
            .body(response);
    }

    @DeleteMapping(path = "/{optionId}")
    public ResponseEntity<Void> deleteOption(
        @PathVariable Long productId,
        @PathVariable Long optionId
    ) {
        boolean deleted = optionService.deleteOption(productId, optionId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
