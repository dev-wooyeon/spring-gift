package gift.order;

import gift.notification.infrastructure.KakaoMessageClient;
import gift.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderCharacterizationTest extends IntegrationTestSupport {
    @MockitoBean
    KakaoMessageClient kakaoMessageClient;

    @Test
    @DisplayName("주문 목록 조회는 인증된 회원의 주문 목록을 반환한다")
    void getOrdersReturnsAuthenticatedMembersOrders() throws Exception {
        // when & then
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
    @DisplayName("주문 목록 조회는 인증 헤더가 없으면 400을 반환한다")
    void getOrdersWithoutAuthorizationHeaderReturnsBadRequest() throws Exception {
        // when & then
        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주문 목록 조회는 토큰이 유효하지 않으면 401을 반환한다")
    void getOrdersWithInvalidTokenReturnsUnauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("주문 생성은 옵션 재고를 차감하고 포인트를 차감한 뒤 주문을 저장한다")
    void createOrderSubtractsOptionQuantityDeductsPointAndPersistsOrder() throws Exception {
        // given
        int beforeQuantity = optionQuantity(5);
        int beforePoint = memberPoint("user1@example.com");
        int beforeOrderCount = orderCount(2);
        Long wishId = insertWish(2, 3);

        // when
        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(5, 2, "주문 테스트")))
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/orders/")))
            .andExpect(jsonPath("$.optionId").value(5))
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.message").value("주문 테스트"));

        // then
        assertThat(optionQuantity(5)).isEqualTo(beforeQuantity - 2);
        assertThat(memberPoint("user1@example.com")).isEqualTo(beforePoint - 358000);
        assertThat(orderCount(2)).isEqualTo(beforeOrderCount + 1);
        assertThat(wishCountById(wishId)).isEqualTo(1);
    }

    @Test
    @DisplayName("주문 생성은 옵션이 없으면 404를 반환한다")
    void createOrderReturnsNotFoundWhenOptionDoesNotExist() throws Exception {
        // when & then
        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(99999, 1, "없는 옵션")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("주문 생성은 수량이 유효하지 않으면 400을 반환한다")
    void createOrderRejectsInvalidQuantity() throws Exception {
        // when & then
        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(5, 0, "수량 오류")))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주문 생성은 재고가 부족하면 예외가 발생한다")
    void createOrderReturnsBadRequestWhenStockIsNotEnough() throws Exception {
        // when & then
        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(8, 999, "재고 부족")))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("옵션 재고가 부족합니다. 차감 수량이 현재 재고보다 많습니다."));
    }

    @Test
    @DisplayName("주문 생성은 포인트가 부족하면 차감된 옵션 재고를 롤백한다")
    void createOrderRollsBackStockWhenPointIsNotEnough() throws Exception {
        // given
        int beforeQuantity = optionQuantity(1);

        // when
        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization("user2@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(1, 1, "포인트 부족")))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("회원 포인트가 부족합니다."));

        // then
        assertThat(optionQuantity(1)).isEqualTo(beforeQuantity);
    }

    @Test
    @DisplayName("주문 생성은 회원에게 카카오 접근 토큰이 있으면 카카오 메시지를 전송한다")
    void createOrderSendsKakaoMessageWhenMemberHasAccessToken() throws Exception {
        // given
        jdbcTemplate.update(
            "update member set kakao_access_token = ? where email = ?",
            "kakao-token-for-order",
            "user1@example.com"
        );

        // when
        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization("user1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(7, 1, "카카오 메시지")))
            .andExpect(status().isCreated());

        // then
        verify(kakaoMessageClient).sendToMe(eq("kakao-token-for-order"), any());
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
