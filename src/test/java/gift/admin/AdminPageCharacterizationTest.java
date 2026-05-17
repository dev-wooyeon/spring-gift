package gift.admin;

import gift.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("관리자 상품 목록 화면은 인증 없이 접근할 수 있다")
    void adminProductListIsAccessibleWithoutAuthentication() throws Exception {
        // when & then
        mockMvc.perform(get("/admin/products"))
            .andExpect(status().isOk())
            .andExpect(view().name("product/list"))
            .andExpect(content().string(containsString("상품 관리")))
            .andExpect(content().string(containsString("맥북 프로 16인치")));
    }

    @Test
    @DisplayName("관리자 상품 추가 화면은 인증 없이 접근할 수 있다")
    void adminProductNewFormIsAccessibleWithoutAuthentication() throws Exception {
        // when & then
        mockMvc.perform(get("/admin/products/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("product/new"))
            .andExpect(content().string(containsString("상품 추가")))
            .andExpect(content().string(containsString("전자기기")));
    }

    @Test
    @DisplayName("관리자 상품 생성은 카카오가 포함된 이름을 허용하고 목록으로 리다이렉트한다")
    void adminProductCreateAllowsKakaoNameAndRedirects() throws Exception {
        // when
        mockMvc.perform(post("/admin/products")
                .param("name", "카카오관리자상품")
                .param("price", "15000")
                .param("imageUrl", "https://example.com/admin-product.jpg")
                .param("categoryId", "1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/products"));

        // then
        assertThat(productCountByName("카카오관리자상품")).isEqualTo(1);
    }

    @Test
    @DisplayName("관리자 상품 생성은 유효하지 않은 이름이면 오류와 함께 입력 화면을 다시 보여준다")
    void adminProductCreateReturnsFormWithErrorsForInvalidName() throws Exception {
        // when
        mockMvc.perform(post("/admin/products")
                .param("name", "관리자상품!")
                .param("price", "15000")
                .param("imageUrl", "https://example.com/admin-invalid-product.jpg")
                .param("categoryId", "1"))
            .andExpect(status().isOk())
            .andExpect(view().name("product/new"))
            .andExpect(model().attributeExists("errors"))
            .andExpect(content().string(containsString("상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다.")));

        // then
        assertThat(productCountByName("관리자상품!")).isZero();
    }

    @Test
    @DisplayName("관리자 상품 수정 화면은 현재 상품 정보를 보여준다")
    void adminProductEditFormShowsCurrentProduct() throws Exception {
        // when & then
        mockMvc.perform(get("/admin/products/1/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("product/edit"))
            .andExpect(content().string(containsString("상품 수정")))
            .andExpect(content().string(containsString("맥북 프로 16인치")));
    }

    @Test
    @DisplayName("관리자 상품 수정은 저장된 상품 정보를 변경하고 목록으로 리다이렉트한다")
    void adminProductUpdateChangesPersistedProductAndRedirects() throws Exception {
        // given
        Long productId = insertProduct("관리자수정전", 1000, "https://example.com/admin-before.jpg", 1);

        // when
        mockMvc.perform(post("/admin/products/" + productId + "/edit")
                .param("name", "관리자수정후")
                .param("price", "2000")
                .param("imageUrl", "https://example.com/admin-after.jpg")
                .param("categoryId", "2"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/products"));

        // then
        assertThat(productCountByName("관리자수정전")).isZero();
        assertThat(productCountByName("관리자수정후")).isEqualTo(1);
    }

    @Test
    @DisplayName("관리자 상품 삭제는 저장된 상품을 제거하고 목록으로 리다이렉트한다")
    void adminProductDeleteRemovesProductAndRedirects() throws Exception {
        // given
        Long productId = insertProduct("관리자삭제상품", 1000, "https://example.com/admin-delete.jpg", 1);

        // when
        mockMvc.perform(post("/admin/products/" + productId + "/delete"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/products"));

        // then
        assertThat(productCountByName("관리자삭제상품")).isZero();
    }

    @Test
    @DisplayName("관리자 회원 목록 화면은 인증 없이 접근할 수 있다")
    void adminMemberListIsAccessibleWithoutAuthentication() throws Exception {
        // when & then
        mockMvc.perform(get("/admin/members"))
            .andExpect(status().isOk())
            .andExpect(view().name("member/list"))
            .andExpect(content().string(containsString("회원 관리")))
            .andExpect(content().string(containsString("user1@example.com")));
    }

    @Test
    @DisplayName("관리자 회원 추가 화면은 인증 없이 접근할 수 있다")
    void adminMemberNewFormIsAccessibleWithoutAuthentication() throws Exception {
        // when & then
        mockMvc.perform(get("/admin/members/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("member/new"))
            .andExpect(content().string(containsString("회원 추가")));
    }

    @Test
    @DisplayName("관리자 회원 생성은 회원을 저장하고 목록으로 리다이렉트한다")
    void adminMemberCreatePersistsMemberAndRedirects() throws Exception {
        // when
        mockMvc.perform(post("/admin/members")
                .param("email", "admin-created@example.com")
                .param("password", "admin-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/members"));

        // then
        assertThat(memberCountByEmail("admin-created@example.com")).isEqualTo(1);
    }

    @Test
    @DisplayName("관리자 회원 생성은 중복 이메일이면 오류를 보여준다")
    void adminMemberCreateShowsErrorForDuplicateEmail() throws Exception {
        // when & then
        mockMvc.perform(post("/admin/members")
                .param("email", "user1@example.com")
                .param("password", "password1"))
            .andExpect(status().isOk())
            .andExpect(view().name("member/new"))
            .andExpect(model().attribute("email", "user1@example.com"))
            .andExpect(content().string(containsString("회원 이메일이 이미 등록되어 있습니다.")));
    }

    @Test
    @DisplayName("관리자 회원 수정 화면은 현재 비밀번호 값을 보여준다")
    void adminMemberEditFormRendersCurrentPasswordValue() throws Exception {
        // when & then
        mockMvc.perform(get("/admin/members/2/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("member/edit"))
            .andExpect(content().string(containsString("회원 수정")))
            .andExpect(content().string(containsString("value=\"password1\"")));
    }

    @Test
    @DisplayName("관리자 회원 수정은 회원 정보를 변경하고 목록으로 리다이렉트한다")
    void adminMemberUpdateChangesMemberAndRedirects() throws Exception {
        // given
        Long memberId = insertMember("admin-before@example.com", "before-password", 0);

        // when
        mockMvc.perform(post("/admin/members/" + memberId + "/edit")
                .param("email", "admin-after@example.com")
                .param("password", "after-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/members"));

        // then
        assertThat(memberCountByEmail("admin-before@example.com")).isZero();
        assertThat(memberCountByEmail("admin-after@example.com")).isEqualTo(1);
    }

    @Test
    @DisplayName("관리자 회원 포인트 충전은 회원 포인트를 증가시키고 목록으로 리다이렉트한다")
    void adminMemberChargePointIncreasesPointAndRedirects() throws Exception {
        // given
        Long memberId = insertMember("charge-target@example.com", "password", 100);

        // when
        mockMvc.perform(post("/admin/members/" + memberId + "/charge-point").param("amount", "250"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/members"));

        // then
        assertThat(memberPoint("charge-target@example.com")).isEqualTo(350);
    }

    @Test
    @DisplayName("관리자 회원 삭제는 저장된 회원을 제거하고 목록으로 리다이렉트한다")
    void adminMemberDeleteRemovesMemberAndRedirects() throws Exception {
        // given
        Long memberId = insertMember("delete-target@example.com", "password", 0);

        // when
        mockMvc.perform(post("/admin/members/" + memberId + "/delete"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/admin/members"));

        // then
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
