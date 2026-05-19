package gift.catalog.application;

import gift.catalog.domain.Category;
import gift.catalog.exception.CatalogException;
import gift.catalog.infrastructure.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findCategory(Long id) {
        return categoryRepository.findById(id);
    }

    public Category getCategory(Long id) {
        return findCategory(id)
            .orElseThrow(() -> CatalogException.notFound("카테고리를 찾을 수 없습니다. id=" + id));
    }

    @Transactional
    public Category createCategory(CategoryCommand command) {
        return categoryRepository.save(new Category(
            command.name(),
            command.color(),
            command.imageUrl(),
            command.description()
        ));
    }

    @Transactional
    public Optional<Category> updateCategory(Long id, CategoryCommand command) {
        return findCategory(id)
            .map(category -> {
                category.update(command.name(), command.color(), command.imageUrl(), command.description());
                return categoryRepository.save(category);
            });
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
