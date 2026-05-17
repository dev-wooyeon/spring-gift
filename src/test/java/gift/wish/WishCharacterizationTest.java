package gift.wish;

import gift.support.IntegrationTestSupport;
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
    void getWishesReturnsAuthenticatedMembersWishes() throws Exception {
        mockMvc.perform(get("/api/wishes")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].productId").value(1))
            .andExpect(jsonPath("$.content[0].name").value("맥북 프로 16인치"))
            .andExpect(jsonPath("$.content[1].productId").value(3))
            .andExpect(jsonPath("$.content[1].name").value("나이키 에어맥스"));
    }

    @Test
    void getWishesWithoutAuthorizationHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/wishes"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getWishesWithInvalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/wishes").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void addWishCreatesWishForAuthenticatedMember() throws Exception {
        mockMvc.perform(post("/api/wishes")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(wishJson(2)))
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/wishes/")))
            .andExpect(jsonPath("$.productId").value(2))
            .andExpect(jsonPath("$.name").value("아이폰 16"));

        assertThat(wishCount(2, 2)).isEqualTo(1);
    }

    @Test
    void addWishReturnsExistingWishWhenAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/wishes")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(wishJson(1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId").value(1))
            .andExpect(jsonPath("$.name").value("맥북 프로 16인치"));

        assertThat(wishCount(2, 1)).isEqualTo(1);
    }

    @Test
    void addWishReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/wishes")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(wishJson(99999)))
            .andExpect(status().isNotFound());
    }

    @Test
    void removeWishDeletesOwnWish() throws Exception {
        Long wishId = insertWish(2, 4);

        mockMvc.perform(delete("/api/wishes/" + wishId)
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com")))
            .andExpect(status().isNoContent());

        assertThat(wishCountById(wishId)).isZero();
    }

    @Test
    void removeWishReturnsForbiddenForAnotherMembersWish() throws Exception {
        Long wishId = insertWish(3, 6);

        mockMvc.perform(delete("/api/wishes/" + wishId)
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com")))
            .andExpect(status().isForbidden());

        assertThat(wishCountById(wishId)).isEqualTo(1);
    }

    @Test
    void removeWishReturnsNotFoundWhenWishDoesNotExist() throws Exception {
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
