package gift.option;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
public class OptionController {
    private final OptionService optionService;

    public OptionController(OptionService optionService) {
        this.optionService = optionService;
    }

    @GetMapping
    public ResponseEntity<List<OptionResponse>> getOptions(@PathVariable Long productId) {
        Optional<List<OptionResponse>> options = optionService.getOptions(productId);
        if (options.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(options.get());
    }

    @PostMapping
    public ResponseEntity<OptionResponse> createOption(
        @PathVariable Long productId,
        @Valid @RequestBody OptionRequest request
    ) {
        Optional<OptionResponse> saved = optionService.createOption(productId, request);
        if (saved.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        OptionResponse response = saved.get();
        URI location = URI.create("/api/products/" + productId + "/options/" + response.id());
        return ResponseEntity.created(location)
            .body(response);
    }

    @DeleteMapping(path = "/{optionId}")
    public ResponseEntity<Void> deleteOption(
        @PathVariable Long productId,
        @PathVariable Long optionId
    ) {
        OptionService.DeleteResult result = optionService.deleteOption(productId, optionId);
        if (result.status() == OptionService.DeleteStatus.NOT_FOUND) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
