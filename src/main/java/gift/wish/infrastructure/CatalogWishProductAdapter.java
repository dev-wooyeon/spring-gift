package gift.wish.infrastructure;

import gift.catalog.application.ProductService;
import gift.catalog.domain.Product;
import gift.wish.application.WishProduct;
import gift.wish.application.WishProductPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing {@link WishProductPort} to bridge the {@code wish} and {@code catalog} domains.
 *
 * <p><strong>Design Decision / Dependency Direction Rationale:</strong>
 * According to Domain-Driven Design (DDD) boundaries, {@code catalog} is a core Bounded Context,
 * whereas {@code wish} is a supporting one. To keep the core {@code catalog} package clean and reusable,
 * we place this Adapter in the client's infrastructure package ({@code gift.wish.infrastructure}).
 * This ensures that the package dependency flows strictly from {@code wish} to {@code catalog} (wish -> catalog),
 * preventing the core catalog logic from being coupled to secondary feature requirements like wish lists.
 */
@Component
@RequiredArgsConstructor
public class CatalogWishProductAdapter implements WishProductPort {
    private final ProductService productService;

    @Override
    public Optional<WishProduct> findProduct(Long productId) {
        return productService.findProduct(productId)
            .map(this::toWishProduct);
    }

    @Override
    public List<WishProduct> findProducts(List<Long> productIds) {
        return productService.findProducts(productIds).stream()
            .map(this::toWishProduct)
            .collect(Collectors.toList());
    }

    private WishProduct toWishProduct(Product product) {
        return new WishProduct(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getImageUrl()
        );
    }
}

