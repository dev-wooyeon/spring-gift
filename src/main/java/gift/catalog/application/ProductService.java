package gift.catalog.application;

import gift.catalog.domain.Category;
import gift.catalog.domain.Product;
import gift.catalog.exception.CatalogException;
import gift.catalog.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public Page<Product> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Optional<Product> getProduct(Long id) {
        return productRepository.findById(id);
    }

    public Optional<Product> findProduct(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> findProducts(List<Long> ids) {
        return productRepository.findAllById(ids);
    }

    public List<Product> getAdminProducts() {
        return productRepository.findAll();
    }

    public Product getAdminProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Product not found with id: {}", id);
                return CatalogException.notFound("상품을 찾을 수 없습니다.");
            });
    }

    @Transactional
    public Optional<Product> createProduct(ProductCommand command) {
        Optional<Category> category = categoryService.findCategory(command.categoryId());
        if (category.isEmpty()) {
            return Optional.empty();
        }

        Product saved = productRepository.save(new Product(
            command.name(),
            command.price(),
            command.imageUrl(),
            category.get()
        ));
        return Optional.of(saved);
    }

    @Transactional
    public Optional<Product> updateProduct(Long id, ProductCommand command) {
        Optional<Category> category = categoryService.findCategory(command.categoryId());
        if (category.isEmpty()) {
            return Optional.empty();
        }

        Optional<Product> product = productRepository.findById(id);
        if (product.isEmpty()) {
            return Optional.empty();
        }

        Product updated = product.get();
        updated.update(command.name(), command.price(), command.imageUrl(), category.get());
        Product saved = productRepository.save(updated);
        return Optional.of(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public List<String> validateAdminName(String name) {
        return Product.checkErrors(name, true);
    }

    @Transactional
    public Product createAdminProduct(String name, int price, String imageUrl, Long categoryId) {
        Category category = categoryService.getCategory(categoryId);
        return productRepository.save(new Product(name, price, imageUrl, category, true));
    }

    @Transactional
    public Product updateAdminProduct(Long id, String name, int price, String imageUrl, Long categoryId) {
        Product product = getAdminProduct(id);
        Category category = categoryService.getCategory(categoryId);
        product.update(name, price, imageUrl, category, true);
        return productRepository.save(product);
    }
}
