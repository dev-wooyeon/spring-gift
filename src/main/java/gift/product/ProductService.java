package gift.product;

import gift.category.Category;
import gift.category.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository, CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
    }

    public Page<ProductResponse> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductResponse::from);
    }

    public Optional<ProductResponse> getProduct(Long id) {
        return productRepository.findById(id).map(ProductResponse::from);
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
    public Optional<ProductResponse> createProduct(ProductRequest request) {
        validateApiName(request.name());

        Optional<Category> category = categoryService.findCategory(request.categoryId());
        if (category.isEmpty()) {
            return Optional.empty();
        }

        Product saved = productRepository.save(request.toEntity(category.get()));
        return Optional.of(ProductResponse.from(saved));
    }

    @Transactional
    public Optional<ProductResponse> updateProduct(Long id, ProductRequest request) {
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
        return Optional.of(ProductResponse.from(saved));
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
