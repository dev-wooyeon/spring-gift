package gift;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AdminPageCharacterizationTest extends IntegrationTestSupport {
    @Test
    void adminProductListIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/admin/products"))
            .andExpect(status().isOk())
            .andExpect(view().name("product/list"))
            .andExpect(content().string(containsString("상품 관리")))
            .andExpect(content().string(containsString("맥북 프로 16인치")));
    }

    @Test
    void adminProductNewFormIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/admin/products/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("product/new"))
            .andExpect(content().string(containsString("상품 추가")))
            .andExpect(content().string(containsString("전자기기")));
    }

    @Test
    void adminProductCreateAllowsKakaoNameAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/products")
                .param("name", "카카오관리자상품")
                .param("price", "15000")
                .param("imageUrl", "https://example.com/admin-product.jpg")
                .param("categoryId", "1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/products"));

        assertThat(productCountByName("카카오관리자상품")).isEqualTo(1);
    }

    @Test
    void adminProductCreateReturnsFormWithErrorsForInvalidName() throws Exception {
        mockMvc.perform(post("/admin/products")
                .param("name", "관리자상품!")
                .param("price", "15000")
                .param("imageUrl", "https://example.com/admin-invalid-product.jpg")
                .param("categoryId", "1"))
            .andExpect(status().isOk())
            .andExpect(view().name("product/new"))
            .andExpect(model().attributeExists("errors"))
            .andExpect(content().string(containsString("상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다.")));

        assertThat(productCountByName("관리자상품!")).isZero();
    }

    @Test
    void adminProductEditFormShowsCurrentProduct() throws Exception {
        mockMvc.perform(get("/admin/products/1/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("product/edit"))
            .andExpect(content().string(containsString("상품 수정")))
            .andExpect(content().string(containsString("맥북 프로 16인치")));
    }

    @Test
    void adminProductUpdateChangesPersistedProductAndRedirects() throws Exception {
        Long productId = insertProduct("관리자수정전", 1000, "https://example.com/admin-before.jpg", 1);

        mockMvc.perform(post("/admin/products/" + productId + "/edit")
                .param("name", "관리자수정후")
                .param("price", "2000")
                .param("imageUrl", "https://example.com/admin-after.jpg")
                .param("categoryId", "2"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/products"));

        assertThat(productCountByName("관리자수정전")).isZero();
        assertThat(productCountByName("관리자수정후")).isEqualTo(1);
    }

    @Test
    void adminProductDeleteRemovesProductAndRedirects() throws Exception {
        Long productId = insertProduct("관리자삭제상품", 1000, "https://example.com/admin-delete.jpg", 1);

        mockMvc.perform(post("/admin/products/" + productId + "/delete"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/products"));

        assertThat(productCountByName("관리자삭제상품")).isZero();
    }

    @Test
    void adminMemberListIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/admin/members"))
            .andExpect(status().isOk())
            .andExpect(view().name("member/list"))
            .andExpect(content().string(containsString("회원 관리")))
            .andExpect(content().string(containsString("user1@example.com")));
    }

    @Test
    void adminMemberNewFormIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/admin/members/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("member/new"))
            .andExpect(content().string(containsString("회원 추가")));
    }

    @Test
    void adminMemberCreatePersistsMemberAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/members")
                .param("email", "admin-created@example.com")
                .param("password", "admin-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/members"));

        assertThat(memberCountByEmail("admin-created@example.com")).isEqualTo(1);
    }

    @Test
    void adminMemberCreateShowsErrorForDuplicateEmail() throws Exception {
        mockMvc.perform(post("/admin/members")
                .param("email", "user1@example.com")
                .param("password", "password1"))
            .andExpect(status().isOk())
            .andExpect(view().name("member/new"))
            .andExpect(model().attribute("email", "user1@example.com"))
            .andExpect(content().string(containsString("Email is already registered.")));
    }

    @Test
    void adminMemberEditFormRendersCurrentPasswordValue() throws Exception {
        mockMvc.perform(get("/admin/members/2/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("member/edit"))
            .andExpect(content().string(containsString("회원 수정")))
            .andExpect(content().string(containsString("value=\"password1\"")));
    }

    @Test
    void adminMemberUpdateChangesMemberAndRedirects() throws Exception {
        Long memberId = insertMember("admin-before@example.com", "before-password", 0);

        mockMvc.perform(post("/admin/members/" + memberId + "/edit")
                .param("email", "admin-after@example.com")
                .param("password", "after-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/members"));

        assertThat(memberCountByEmail("admin-before@example.com")).isZero();
        assertThat(memberCountByEmail("admin-after@example.com")).isEqualTo(1);
    }

    @Test
    void adminMemberChargePointIncreasesPointAndRedirects() throws Exception {
        Long memberId = insertMember("charge-target@example.com", "password", 100);

        mockMvc.perform(post("/admin/members/" + memberId + "/charge-point").param("amount", "250"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/members"));

        assertThat(memberPoint("charge-target@example.com")).isEqualTo(350);
    }

    @Test
    void adminMemberDeleteRemovesMemberAndRedirects() throws Exception {
        Long memberId = insertMember("delete-target@example.com", "password", 0);

        mockMvc.perform(post("/admin/members/" + memberId + "/delete"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/members"));

        assertThat(memberCountByEmail("delete-target@example.com")).isZero();
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

    private Long insertMember(String email, String password, int point) {
        jdbcTemplate.update(
            "insert into member (email, password, point) values (?, ?, ?)",
            email,
            password,
            point
        );
        return jdbcTemplate.queryForObject("select id from member where email = ?", Long.class, email);
    }

    private Integer memberCountByEmail(String email) {
        return jdbcTemplate.queryForObject("select count(*) from member where email = ?", Integer.class, email);
    }

    private Integer memberPoint(String email) {
        return jdbcTemplate.queryForObject("select point from member where email = ?", Integer.class, email);
    }
}
