package gift.product;

import gift.category.Category;
import gift.category.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

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

    public List<Product> getAdminProducts() {
        return productRepository.findAll();
    }

    public Product getAdminProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
    }

    @Transactional
    public Optional<Product> createProduct(ProductRequest request) {
        validateApiName(request.name());

        Optional<Category> category = categoryService.findCategory(request.categoryId());
        if (category.isEmpty()) {
            return Optional.empty();
        }

        Product saved = productRepository.save(request.toEntity(category.get()));
        return Optional.of(saved);
    }

    @Transactional
    public Optional<Product> updateProduct(Long id, ProductRequest request) {
        validateApiName(request.name());

        Optional<Category> category = categoryService.findCategory(request.categoryId());
        if (category.isEmpty()) {
            return Optional.empty();
        }

        Optional<Product> product = productRepository.findById(id);
        if (product.isEmpty()) {
            return Optional.empty();
        }

        Product updated = product.get();
        updated.update(request.name(), request.price(), request.imageUrl(), category.get());
        Product saved = productRepository.save(updated);
        return Optional.of(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public List<String> validateAdminName(String name) {
        return ProductNameValidator.validate(name, true);
    }

    @Transactional
    public Product createAdminProduct(String name, int price, String imageUrl, Long categoryId) {
        Category category = categoryService.getCategory(categoryId);
        return productRepository.save(new Product(name, price, imageUrl, category));
    }

    @Transactional
    public Product updateAdminProduct(Long id, String name, int price, String imageUrl, Long categoryId) {
        Product product = getAdminProduct(id);
        Category category = categoryService.getCategory(categoryId);
        product.update(name, price, imageUrl, category);
        return productRepository.save(product);
    }

    private void validateApiName(String name) {
        List<String> errors = ProductNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
