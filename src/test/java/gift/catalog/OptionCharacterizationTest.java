package gift.catalog;

import gift.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OptionCharacterizationTest extends IntegrationTestSupport {
    @Test
    @DisplayName("옵션 목록 조회는 상품의 옵션 목록을 반환한다")
    void getOptionsReturnsProductsOptions() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products/1/options"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("스페이스 블랙 / M1 Pro"))
            .andExpect(jsonPath("$[0].quantity").value(10))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("실버 / M1 Max"))
            .andExpect(jsonPath("$[1].quantity").value(5));
    }

    @Test
    @DisplayName("옵션 목록 조회는 상품이 없으면 404를 반환한다")
    void getOptionsReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products/99999/options"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("옵션 생성은 상품이 있으면 옵션을 저장하고 201을 반환한다")
    void createOptionPersistsOption() throws Exception {
        // given
        Long productId = insertProduct("옵션생성상품");

        // when
        mockMvc.perform(post("/api/products/" + productId + "/options")
                .contentType(MediaType.APPLICATION_JSON)
                .content(optionJson("신규 옵션", 12)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/api/products/" + productId + "/options/")))
            .andExpect(jsonPath("$.name").value("신규 옵션"))
            .andExpect(jsonPath("$.quantity").value(12));

        // then
        assertThat(optionCount(productId, "신규 옵션")).isEqualTo(1);
    }

    @Test
    @DisplayName("옵션 생성은 상품이 없으면 404를 반환한다")
    void createOptionReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        // when & then
        mockMvc.perform(post("/api/products/99999/options")
                .contentType(MediaType.APPLICATION_JSON)
                .content(optionJson("없는 상품 옵션", 12)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("옵션 생성은 같은 상품에 같은 이름이 있으면 400을 반환하고 저장하지 않는다")
    void createOptionRejectsDuplicateNameForSameProduct() throws Exception {
        // given
        Long productId = insertProduct("옵션중복상품");
        insertOption(productId, "중복 옵션", 3);

        // when
        mockMvc.perform(post("/api/products/" + productId + "/options")
            .contentType(MediaType.APPLICATION_JSON)
            .content(optionJson("중복 옵션", 9)))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("해당 상품에 이미 존재하는 옵션 이름입니다."));

        // then
        assertThat(optionCount(productId, "중복 옵션")).isEqualTo(1);
    }

    @Test
    @DisplayName("옵션은 같은 상품에 같은 이름을 데이터베이스 제약으로 중복 저장할 수 없다")
    void optionNameIsUniquePerProduct() {
        // given
        Long productId = insertProduct("옵션유니크상품");
        insertOption(productId, "유니크 옵션", 3);

        // when & then
        assertThatThrownBy(() -> insertOption(productId, "유니크 옵션", 9))
            .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(optionCount(productId, "유니크 옵션")).isEqualTo(1);
    }

    @Test
    @DisplayName("옵션 생성은 이름이 유효하지 않으면 400을 반환하고 저장하지 않는다")
    void createOptionRejectsInvalidName() throws Exception {
        // given
        Long productId = insertProduct("옵션이름오류상품");

        // when
        mockMvc.perform(post("/api/products/" + productId + "/options")
                .contentType(MediaType.APPLICATION_JSON)
                .content(optionJson("옵션!", 12)))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _"));

        // then
        assertThat(optionCount(productId, "옵션!")).isZero();
    }

    @Test
    @DisplayName("옵션 생성은 수량이 유효하지 않으면 400을 반환하고 저장하지 않는다")
    void createOptionRejectsInvalidQuantity() throws Exception {
        // given
        Long productId = insertProduct("옵션수량오류상품");

        // when
        mockMvc.perform(post("/api/products/" + productId + "/options")
                .contentType(MediaType.APPLICATION_JSON)
            .content(optionJson("수량 오류 옵션", 0)))
            .andExpect(status().isBadRequest());

        // then
        assertThat(optionCount(productId, "수량 오류 옵션")).isZero();
    }

    @Test
    @DisplayName("옵션 삭제는 상품에 여러 옵션이 있으면 대상 옵션을 삭제한다")
    void deleteOptionRemovesOptionWhenProductHasMultipleOptions() throws Exception {
        // given
        Long productId = insertProduct("옵션삭제상품");
        insertOption(productId, "유지 옵션", 3);
        Long optionId = insertOption(productId, "삭제 옵션", 4);

        // when
        mockMvc.perform(delete("/api/products/" + productId + "/options/" + optionId))
            .andExpect(status().isNoContent());

        // then
        assertThat(optionCount(productId, "삭제 옵션")).isZero();
        assertThat(optionCount(productId, "유지 옵션")).isEqualTo(1);
    }

    @Test
    @DisplayName("옵션 삭제는 상품이 없으면 404를 반환한다")
    void deleteOptionReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/products/99999/options/1"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("옵션 삭제는 상품의 마지막 옵션이면 400을 반환하고 삭제하지 않는다")
    void deleteOptionRejectsWhenProductHasOnlyOneOption() throws Exception {
        // given
        Long productId = insertProduct("마지막옵션상품");
        Long optionId = insertOption(productId, "마지막 옵션", 3);

        // when
        mockMvc.perform(delete("/api/products/" + productId + "/options/" + optionId))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("상품에는 최소 1개의 옵션이 필요합니다."));

        // then
        assertThat(optionCount(productId, "마지막 옵션")).isEqualTo(1);
    }

    @Test
    @DisplayName("옵션 삭제는 다른 상품의 옵션이면 404를 반환하고 삭제하지 않는다")
    void deleteOptionReturnsNotFoundWhenOptionBelongsToAnotherProduct() throws Exception {
        // given
        Long productId = insertProduct("옵션소유자상품");
        insertOption(productId, "첫 번째 옵션", 3);
        insertOption(productId, "두 번째 옵션", 4);

        Long otherProductId = insertProduct("다른옵션상품");
        Long otherOptionId = insertOption(otherProductId, "다른 상품 옵션", 5);

        // when
        mockMvc.perform(delete("/api/products/" + productId + "/options/" + otherOptionId))
            .andExpect(status().isNotFound());

        // then
        assertThat(optionCount(otherProductId, "다른 상품 옵션")).isEqualTo(1);
    }

    private Long insertProduct(String name) {
        jdbcTemplate.update(
            "insert into product (name, price, image_url, category_id) values (?, ?, ?, ?)",
            name,
            1000,
            "https://example.com/" + name + ".jpg",
            1
        );
        return jdbcTemplate.queryForObject("select id from product where name = ?", Long.class, name);
    }

    private Long insertOption(Long productId, String name, int quantity) {
        jdbcTemplate.update(
            "insert into options (product_id, name, quantity) values (?, ?, ?)",
            productId,
            name,
            quantity
        );
        return jdbcTemplate.queryForObject(
            "select id from options where product_id = ? and name = ?",
            Long.class,
            productId,
            name
        );
    }

    private Integer optionCount(Long productId, String name) {
        return jdbcTemplate.queryForObject(
            "select count(*) from options where product_id = ? and name = ?",
            Integer.class,
            productId,
            name
        );
    }

    private String optionJson(String name, int quantity) {
        return """
            {
              "name": "%s",
              "quantity": %d
            }
            """.formatted(name, quantity);
    }
}
