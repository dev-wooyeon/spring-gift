package gift.wish;

import gift.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WishCharacterizationTest extends IntegrationTestSupport {
    @Test
    @DisplayName("위시 목록 조회는 인증된 회원의 위시 목록을 반환한다")
    void getWishesReturnsAuthenticatedMembersWishes() throws Exception {
        // when & then
        mockMvc.perform(get("/api/wishes")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].productId").value(1))
            .andExpect(jsonPath("$.content[0].name").value("맥북 프로 16인치"))
            .andExpect(jsonPath("$.content[1].productId").value(3))
            .andExpect(jsonPath("$.content[1].name").value("나이키 에어맥스"));
    }

    @Test
    @DisplayName("위시 목록 조회는 인증 헤더가 없으면 400을 반환한다")
    void getWishesWithoutAuthorizationHeaderReturnsBadRequest() throws Exception {
        // when & then
        mockMvc.perform(get("/api/wishes"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("위시 목록 조회는 토큰이 유효하지 않으면 401을 반환한다")
    void getWishesWithInvalidTokenReturnsUnauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/wishes").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("위시 추가는 인증된 회원의 위시를 생성한다")
    void addWishCreatesWishForAuthenticatedMember() throws Exception {
        // when
        mockMvc.perform(post("/api/wishes")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(wishJson(2)))
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/wishes/")))
            .andExpect(jsonPath("$.productId").value(2))
            .andExpect(jsonPath("$.name").value("아이폰 16"));

        // then
        assertThat(wishCount(2, 2)).isEqualTo(1);
    }

    @Test
    @DisplayName("위시 추가는 이미 존재하는 위시면 기존 위시를 반환한다")
    void addWishReturnsExistingWishWhenAlreadyExists() throws Exception {
        // when
        mockMvc.perform(post("/api/wishes")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(wishJson(1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId").value(1))
            .andExpect(jsonPath("$.name").value("맥북 프로 16인치"));

        // then
        assertThat(wishCount(2, 1)).isEqualTo(1);
    }

    @Test
    @DisplayName("위시 추가는 상품이 없으면 404를 반환한다")
    void addWishReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        // when & then
        mockMvc.perform(post("/api/wishes")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(wishJson(99999)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("위시 삭제는 자신의 위시를 삭제한다")
    void removeWishDeletesOwnWish() throws Exception {
        // given
        Long wishId = insertWish(2, 4);

        // when
        mockMvc.perform(delete("/api/wishes/" + wishId)
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com")))
            .andExpect(status().isNoContent());

        // then
        assertThat(wishCountById(wishId)).isZero();
    }

    @Test
    @DisplayName("위시 삭제는 다른 회원의 위시면 403을 반환하고 삭제하지 않는다")
    void removeWishReturnsForbiddenForAnotherMembersWish() throws Exception {
        // given
        Long wishId = insertWish(3, 6);

        // when
        mockMvc.perform(delete("/api/wishes/" + wishId)
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com")))
            .andExpect(status().isForbidden());

        // then
        assertThat(wishCountById(wishId)).isEqualTo(1);
    }

    @Test
    @DisplayName("위시 삭제는 위시가 없으면 404를 반환한다")
    void removeWishReturnsNotFoundWhenWishDoesNotExist() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/wishes/99999")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com")))
            .andExpect(status().isNotFound());
    }

    private Long insertWish(long memberId, long productId) {
        jdbcTemplate.update("insert into wish (member_id, product_id) values (?, ?)", memberId, productId);
        return jdbcTemplate.queryForObject(
            "select id from wish where member_id = ? and product_id = ? order by id desc limit 1",
            Long.class,
            memberId,
            productId
        );
    }

    private Integer wishCount(long memberId, long productId) {
        return jdbcTemplate.queryForObject(
            "select count(*) from wish where member_id = ? and product_id = ?",
            Integer.class,
            memberId,
            productId
        );
    }

    private Integer wishCountById(long wishId) {
        return jdbcTemplate.queryForObject("select count(*) from wish where id = ?", Integer.class, wishId);
    }

    private String wishJson(long productId) {
        return """
            {
              "productId": %d
            }
            """.formatted(productId);
    }
}
