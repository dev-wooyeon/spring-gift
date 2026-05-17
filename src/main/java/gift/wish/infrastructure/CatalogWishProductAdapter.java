package gift.wish.infrastructure;

import gift.catalog.application.ProductService;
import gift.catalog.domain.Product;
import gift.wish.application.WishProduct;
import gift.wish.application.WishProductPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CatalogWishProductAdapter implements WishProductPort {
    private final ProductService productService;

    @Override
    public Optional<WishProduct> findProduct(Long productId) {
        return productService.findProduct(productId)
            .map(this::toWishProduct);
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
