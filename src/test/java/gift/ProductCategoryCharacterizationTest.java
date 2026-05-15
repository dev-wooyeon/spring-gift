package gift;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductCategoryCharacterizationTest extends IntegrationTestSupport {
    @Test
    void getProductsReturnsDefaultProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].name").value("맥북 프로 16인치"))
            .andExpect(jsonPath("$.content[0].price").value(3360000))
            .andExpect(jsonPath("$.content[0].categoryId").value(1));
    }

    @Test
    void getProductReturnsDefaultProduct() throws Exception {
        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("맥북 프로 16인치"))
            .andExpect(jsonPath("$.price").value(3360000))
            .andExpect(jsonPath("$.imageUrl").value("https://example.com/images/macbook.jpg"))
            .andExpect(jsonPath("$.categoryId").value(1));
    }

    @Test
    void getProductReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/products/99999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createProductPersistsProductWithCategory() throws Exception {
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("테스트상품", 12000, "https://example.com/test-product.jpg", 1)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/api/products/")))
            .andExpect(jsonPath("$.name").value("테스트상품"))
            .andExpect(jsonPath("$.price").value(12000))
            .andExpect(jsonPath("$.categoryId").value(1));

        assertThat(productCountByName("테스트상품")).isEqualTo(1);
    }

    @Test
    void createProductReturnsNotFoundWhenCategoryDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("없는카테고리", 12000, "https://example.com/missing-category.jpg", 99999)))
            .andExpect(status().isNotFound());

        assertThat(productCountByName("없는카테고리")).isZero();
    }

    @Test
    void createProductRejectsForbiddenKakaoName() throws Exception {
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("카카오선물", 12000, "https://example.com/kakao.jpg", 1)))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다."));

        assertThat(productCountByName("카카오선물")).isZero();
    }

    @Test
    void createProductRejectsNonPositivePrice() throws Exception {
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("가격오류", 0, "https://example.com/invalid-price.jpg", 1)))
            .andExpect(status().isBadRequest());

        assertThat(productCountByName("가격오류")).isZero();
    }

    @Test
    void updateProductChangesPersistedProduct() throws Exception {
        Long categoryId = insertCategory("상품수정카테고리");
        Long productId = insertProduct("수정전상품", 1000, "https://example.com/before.jpg", 1);

        mockMvc.perform(put("/api/products/" + productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("수정후상품", 22000, "https://example.com/after.jpg", categoryId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(productId))
            .andExpect(jsonPath("$.name").value("수정후상품"))
            .andExpect(jsonPath("$.price").value(22000))
            .andExpect(jsonPath("$.imageUrl").value("https://example.com/after.jpg"))
            .andExpect(jsonPath("$.categoryId").value(categoryId));

        assertThat(productCountByName("수정전상품")).isZero();
        assertThat(productCountByName("수정후상품")).isEqualTo(1);
    }

    @Test
    void updateProductReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/products/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("수정불가상품", 12000, "https://example.com/missing-product.jpg", 1)))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateProductReturnsNotFoundWhenCategoryDoesNotExist() throws Exception {
        Long productId = insertProduct("카테고리없음수정", 1000, "https://example.com/category-missing-before.jpg", 1);

        mockMvc.perform(put("/api/products/" + productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("카테고리없음수정후", 12000, "https://example.com/category-missing-after.jpg", 99999)))
            .andExpect(status().isNotFound());

        assertThat(productCountByName("카테고리없음수정")).isEqualTo(1);
        assertThat(productCountByName("카테고리없음수정후")).isZero();
    }

    @Test
    void deleteProductRemovesPersistedProduct() throws Exception {
        Long productId = insertProduct("삭제대상상품", 1000, "https://example.com/delete-product.jpg", 1);

        mockMvc.perform(delete("/api/products/" + productId))
            .andExpect(status().isNoContent());

        assertThat(productCountByName("삭제대상상품")).isZero();
    }

    @Test
    void getCategoriesReturnsDefaultCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("전자기기"))
            .andExpect(jsonPath("$[0].color").value("#1E90FF"));
    }

    @Test
    void createCategoryPersistsCategory() throws Exception {
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryJson("테스트카테고리", "#123456", "https://example.com/category.jpg", "테스트 설명")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/api/categories/")))
            .andExpect(jsonPath("$.name").value("테스트카테고리"))
            .andExpect(jsonPath("$.color").value("#123456"))
            .andExpect(jsonPath("$.description").value("테스트 설명"));

        assertThat(categoryCountByName("테스트카테고리")).isEqualTo(1);
    }

    @Test
    void createCategoryRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryJson("", "#123456", "https://example.com/blank-category.jpg", "이름 없음")))
            .andExpect(status().isBadRequest());

        assertThat(categoryCountByName("")).isZero();
    }

    @Test
    void updateCategoryChangesPersistedCategory() throws Exception {
        Long categoryId = insertCategory("수정전카테고리");

        mockMvc.perform(put("/api/categories/" + categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryJson("수정후카테고리", "#ABCDEF", "https://example.com/updated-category.jpg", "수정 설명")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(categoryId))
            .andExpect(jsonPath("$.name").value("수정후카테고리"))
            .andExpect(jsonPath("$.color").value("#ABCDEF"))
            .andExpect(jsonPath("$.imageUrl").value("https://example.com/updated-category.jpg"))
            .andExpect(jsonPath("$.description").value("수정 설명"));

        assertThat(categoryCountByName("수정전카테고리")).isZero();
        assertThat(categoryCountByName("수정후카테고리")).isEqualTo(1);
    }

    @Test
    void updateCategoryReturnsNotFoundWhenCategoryDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/categories/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryJson("없는수정카테고리", "#ABCDEF", "https://example.com/missing-category.jpg", "없음")))
            .andExpect(status().isNotFound());

        assertThat(categoryCountByName("없는수정카테고리")).isZero();
    }

    @Test
    void deleteCategoryRemovesPersistedCategory() throws Exception {
        Long categoryId = insertCategory("삭제대상카테고리");

        mockMvc.perform(delete("/api/categories/" + categoryId))
            .andExpect(status().isNoContent());

        assertThat(categoryCountByName("삭제대상카테고리")).isZero();
    }

    private Long insertCategory(String name) {
        jdbcTemplate.update(
            "insert into category (name, color, image_url, description) values (?, ?, ?, ?)",
            name,
            "#654321",
            "https://example.com/" + name + ".jpg",
            name + " 설명"
        );
        return jdbcTemplate.queryForObject("select id from category where name = ?", Long.class, name);
    }

    private Long insertProduct(String name, int price, String imageUrl, long categoryId) {
        jdbcTemplate.update(
            "insert into product (name, price, image_url, category_id) values (?, ?, ?, ?)",
            name,
            price,
            imageUrl,
            categoryId
        );
        return jdbcTemplate.queryForObject("select id from product where name = ?", Long.class, name);
    }

    private Integer productCountByName(String name) {
        return jdbcTemplate.queryForObject("select count(*) from product where name = ?", Integer.class, name);
    }

    private Integer categoryCountByName(String name) {
        return jdbcTemplate.queryForObject("select count(*) from category where name = ?", Integer.class, name);
    }

    private String productJson(String name, int price, String imageUrl, long categoryId) {
        return """
            {
              "name": "%s",
              "price": %d,
              "imageUrl": "%s",
              "categoryId": %d
            }
            """.formatted(name, price, imageUrl, categoryId);
    }

    private String categoryJson(String name, String color, String imageUrl, String description) {
        return """
            {
              "name": "%s",
              "color": "%s",
              "imageUrl": "%s",
              "description": "%s"
            }
            """.formatted(name, color, imageUrl, description);
    }
}
