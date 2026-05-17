package gift.catalog.presentation;

import gift.catalog.application.CategoryService;
import gift.catalog.domain.Category;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories().stream()
            .map(CategoryResponse::from)
            .toList();
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        Category saved = categoryService.createCategory(request.toCommand());
        return ResponseEntity.created(URI.create("/api/categories/" + saved.getId()))
            .body(CategoryResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
        @PathVariable Long id,
        @Valid @RequestBody CategoryRequest request
    ) {
        Optional<Category> category = categoryService.updateCategory(id, request.toCommand());
        if (category.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(CategoryResponse.from(category.get()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
