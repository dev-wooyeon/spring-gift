package gift.catalog.presentation;

import gift.catalog.application.ProductService;
import gift.catalog.domain.Product;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(Pageable pageable) {
        return ResponseEntity.ok(productService.getProducts(pageable).map(ProductResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        Optional<Product> product = productService.getProduct(id);
        if (product.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ProductResponse.from(product.get()));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        Optional<Product> saved = productService.createProduct(request.toCommand());
        if (saved.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ProductResponse response = ProductResponse.from(saved.get());
        return ResponseEntity.created(URI.create("/api/products/" + response.id()))
            .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
        @PathVariable Long id,
        @Valid @RequestBody ProductRequest request
    ) {
        Optional<Product> saved = productService.updateProduct(id, request.toCommand());
        if (saved.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ProductResponse.from(saved.get()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
