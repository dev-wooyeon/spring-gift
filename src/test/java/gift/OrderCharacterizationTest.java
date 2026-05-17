package gift;

import gift.notification.infrastructure.KakaoMessageClient;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderCharacterizationTest extends IntegrationTestSupport {
    @MockitoBean
    KakaoMessageClient kakaoMessageClient;

    @Test
    void getOrdersReturnsAuthenticatedMembersOrders() throws Exception {
        mockMvc.perform(get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].optionId").value(3))
            .andExpect(jsonPath("$.content[0].quantity").value(1))
            .andExpect(jsonPath("$.content[0].message").value("생일 축하해! 🎉"))
            .andExpect(jsonPath("$.content[1].optionId").value(5))
            .andExpect(jsonPath("$.content[1].quantity").value(2));
    }

    @Test
    void getOrdersWithoutAuthorizationHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getOrdersWithInvalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrderSubtractsOptionQuantityDeductsPointAndPersistsOrder() throws Exception {
        int beforeQuantity = optionQuantity(5);
        int beforePoint = memberPoint("user1@example.com");
        int beforeOrderCount = orderCount(2);
        Long wishId = insertWish(2, 3);

        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(5, 2, "주문 테스트")))
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/orders/")))
            .andExpect(jsonPath("$.optionId").value(5))
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.message").value("주문 테스트"));

        assertThat(optionQuantity(5)).isEqualTo(beforeQuantity - 2);
        assertThat(memberPoint("user1@example.com")).isEqualTo(beforePoint - 358000);
        assertThat(orderCount(2)).isEqualTo(beforeOrderCount + 1);
        assertThat(wishCountById(wishId)).isEqualTo(1);
    }

    @Test
    void createOrderReturnsNotFoundWhenOptionDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(99999, 1, "없는 옵션")))
            .andExpect(status().isNotFound());
    }

    @Test
    void createOrderRejectsInvalidQuantity() throws Exception {
        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(5, 0, "수량 오류")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createOrderThrowsWhenStockIsNotEnough() {
        assertThatThrownBy(() -> mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(8, 999, "재고 부족"))))
            .isInstanceOf(ServletException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("차감할 수량이 현재 재고보다 많습니다.");
    }

    @Test
    void createOrderThrowsWhenPointIsNotEnoughAfterSubtractingStock() {
        int beforeQuantity = optionQuantity(1);

        assertThatThrownBy(() -> mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization("user2@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(1, 1, "포인트 부족"))))
            .isInstanceOf(ServletException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("포인트가 부족합니다.");

        assertThat(optionQuantity(1)).isEqualTo(beforeQuantity - 1);
    }

    @Test
    void createOrderSendsKakaoMessageWhenMemberHasAccessToken() throws Exception {
        jdbcTemplate.update(
            "update member set kakao_access_token = ? where email = ?",
            "kakao-token-for-order",
            "user1@example.com"
        );

        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(7, 1, "카카오 메시지")))
            .andExpect(status().isCreated());

        verify(kakaoMessageClient).sendToMe(eq("kakao-token-for-order"), any(), any());
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

    private Integer wishCountById(long wishId) {
        return jdbcTemplate.queryForObject("select count(*) from wish where id = ?", Integer.class, wishId);
    }

    private Integer optionQuantity(long optionId) {
        return jdbcTemplate.queryForObject("select quantity from options where id = ?", Integer.class, optionId);
    }

    private Integer memberPoint(String email) {
        return jdbcTemplate.queryForObject("select point from member where email = ?", Integer.class, email);
    }

    private Integer orderCount(long memberId) {
        return jdbcTemplate.queryForObject("select count(*) from orders where member_id = ?", Integer.class, memberId);
    }

    private String orderJson(long optionId, int quantity, String message) {
        return """
            {
              "optionId": %d,
              "quantity": %d,
              "message": "%s"
            }
            """.formatted(optionId, quantity, message);
    }
}
