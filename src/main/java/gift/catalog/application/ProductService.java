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
        log.info("[ProductService] 상품 페이징 조회 - Pageable: {}", pageable);
        return productRepository.findAll(pageable);
    }

    public Optional<Product> getProduct(Long id) {
        log.info("[ProductService] 상품 상세 조회 - 상품 ID: {}", id);
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
            .orElseThrow(() -> {
                log.error("[ProductService] 어드민 상품 조회 실패 - 존재하지 않는 상품 ID: {}", id);
                return CatalogException.notFound("상품을 찾을 수 없습니다. id=" + id);
            });
    }

    @Transactional
    public Optional<Product> createProduct(ProductCommand command) {
        log.info("[ProductService] 상품 생성 요청 - 상품명: {}, 가격: {}", command.name(), command.price());
        validateApiName(command.name());

        Optional<Category> category = categoryService.findCategory(command.categoryId());
        if (category.isEmpty()) {
            log.error("[ProductService] 상품 생성 실패 - 존재하지 않는 카테고리 ID: {}", command.categoryId());
            return Optional.empty();
        }

        Product saved = productRepository.save(new Product(
            command.name(),
            command.price(),
            command.imageUrl(),
            category.get()
        ));
        log.info("[ProductService] 상품 생성 성공 - 생성된 상품 ID: {}", saved.getId());
        return Optional.of(saved);
    }

    @Transactional
    public Optional<Product> updateProduct(Long id, ProductCommand command) {
        log.info("[ProductService] 상품 수정 요청 - 상품 ID: {}, 새 상품명: {}", id, command.name());
        validateApiName(command.name());

        Optional<Category> category = categoryService.findCategory(command.categoryId());
        if (category.isEmpty()) {
            log.error("[ProductService] 상품 수정 실패 - 존재하지 않는 카테고리 ID: {}", command.categoryId());
            return Optional.empty();
        }

        Optional<Product> product = productRepository.findById(id);
        if (product.isEmpty()) {
            log.error("[ProductService] 상품 수정 실패 - 존재하지 않는 상품 ID: {}", id);
            return Optional.empty();
        }

        Product updated = product.get();
        updated.update(command.name(), command.price(), command.imageUrl(), category.get());
        Product saved = productRepository.save(updated);
        log.info("[ProductService] 상품 수정 성공 - 수정된 상품 ID: {}", saved.getId());
        return Optional.of(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.info("[ProductService] 상품 삭제 요청 - 상품 ID: {}", id);
        productRepository.deleteById(id);
        log.info("[ProductService] 상품 삭제 성공 - 상품 ID: {}", id);
    }

    public List<String> validateAdminName(String name) {
        return Product.validateName(name, true);
    }

    @Transactional
    public Product createAdminProduct(String name, int price, String imageUrl, Long categoryId) {
        log.info("[ProductService] 어드민 상품 생성 - 상품명: {}", name);
        Category category = categoryService.getCategory(categoryId);
        return productRepository.save(new Product(name, price, imageUrl, category, true));
    }

    @Transactional
    public Product updateAdminProduct(Long id, String name, int price, String imageUrl, Long categoryId) {
        log.info("[ProductService] 어드민 상품 수정 - 상품 ID: {}, 상품명: {}", id, name);
        Product product = getAdminProduct(id);
        Category category = categoryService.getCategory(categoryId);
        product.update(name, price, imageUrl, category, true);
        return productRepository.save(product);
    }

    private void validateApiName(String name) {
        List<String> errors = Product.validateName(name, false);
        if (!errors.isEmpty()) {
            log.error("[ProductService] API 상품 이름 검증 실패 - 상품명: {}, 에러: {}", name, errors);
            throw CatalogException.invalid(String.join(", ", errors));
        }
    }
}
