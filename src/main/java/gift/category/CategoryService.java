package gift.category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream()
            .map(CategoryResponse::from)
            .toList();
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findCategory(Long id) {
        return categoryRepository.findById(id);
    }

    public Category getCategory(Long id) {
        return findCategory(id)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + id));
    }

    @Transactional
    public Category createCategory(CategoryRequest request) {
        return categoryRepository.save(request.toEntity());
    }

    @Transactional
    public Optional<Category> updateCategory(Long id, CategoryRequest request) {
        return findCategory(id)
            .map(category -> {
                category.update(request.name(), request.color(), request.imageUrl(), request.description());
                return categoryRepository.save(category);
            });
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
