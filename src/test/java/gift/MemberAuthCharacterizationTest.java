package gift;

import gift.auth.KakaoLoginClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberAuthCharacterizationTest extends IntegrationTestSupport {
    @MockitoBean
    KakaoLoginClient kakaoLoginClient;

    @Test
    void registerCreatesMemberAndReturnsToken() throws Exception {
        String token = mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("new-member@example.com", "password123")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replace("{\"token\":\"", "")
            .replace("\"}", "");

        assertThat(jwtProvider.getEmail(token)).isEqualTo("new-member@example.com");
        assertThat(memberCountByEmail("new-member@example.com")).isEqualTo(1);
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("user1@example.com", "password1")))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Email is already registered."));

        assertThat(memberCountByEmail("user1@example.com")).isEqualTo(1);
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("not-an-email", "password1")))
            .andExpect(status().isBadRequest());

        assertThat(memberCountByEmail("not-an-email")).isZero();
    }

    @Test
    void loginReturnsTokenForExistingMember() throws Exception {
        String token = mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("user1@example.com", "password1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replace("{\"token\":\"", "")
            .replace("\"}", "");

        assertThat(jwtProvider.getEmail(token)).isEqualTo("user1@example.com");
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("user1@example.com", "wrong-password")))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Invalid email or password."));
    }

    @Test
    void kakaoLoginRedirectsToKakaoAuthorizationUrl() throws Exception {
        mockMvc.perform(get("/api/auth/kakao/login"))
            .andExpect(status().isFound())
            .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString(
                "https://kauth.kakao.com/oauth/authorize"
            )))
            .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString("response_type=code")))
            .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString("scope=account_email,talk_message")));
    }

    @Test
    void kakaoCallbackCreatesMemberUpdatesAccessTokenAndReturnsServiceToken() throws Exception {
        when(kakaoLoginClient.requestAccessToken(eq("auth-code")))
            .thenReturn(new KakaoLoginClient.KakaoTokenResponse("kakao-access-token"));
        when(kakaoLoginClient.requestUserInfo(eq("kakao-access-token")))
            .thenReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("kakao-user@example.com")
            ));

        String token = mockMvc.perform(get("/api/auth/kakao/callback").param("code", "auth-code"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replace("{\"token\":\"", "")
            .replace("\"}", "");

        assertThat(jwtProvider.getEmail(token)).isEqualTo("kakao-user@example.com");
        assertThat(memberCountByEmail("kakao-user@example.com")).isEqualTo(1);
        assertThat(kakaoAccessTokenByEmail("kakao-user@example.com")).isEqualTo("kakao-access-token");
    }

    private Integer memberCountByEmail(String email) {
        return jdbcTemplate.queryForObject("select count(*) from member where email = ?", Integer.class, email);
    }

    private String kakaoAccessTokenByEmail(String email) {
        return jdbcTemplate.queryForObject(
            "select kakao_access_token from member where email = ?",
            String.class,
            email
        );
    }

    private String memberJson(String email, String password) {
        return """
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password);
    }
}
