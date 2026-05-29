package gift.auth;

import gift.auth.infrastructure.KakaoLoginClient;
import gift.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("회원 가입은 회원을 생성하고 서비스 토큰을 반환한다")
    void registerCreatesMemberAndReturnsToken() throws Exception {
        // when
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

        // then
        assertThat(jwtProvider.getEmail(token)).isEqualTo("new-member@example.com");
        assertThat(memberCountByEmail("new-member@example.com")).isEqualTo(1);
    }

    @Test
    @DisplayName("회원 가입은 중복 이메일이면 400을 반환하고 회원을 추가하지 않는다")
    void registerRejectsDuplicateEmail() throws Exception {
        // when
        mockMvc.perform(post("/api/members/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(memberJson("user1@example.com", "password1")))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("회원 이메일이 이미 등록되어 있습니다."));

        // then
        assertThat(memberCountByEmail("user1@example.com")).isEqualTo(1);
    }

    @Test
    @DisplayName("회원 가입은 이메일 형식이 유효하지 않으면 400을 반환하고 회원을 추가하지 않는다")
    void registerRejectsInvalidEmail() throws Exception {
        // when
        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("not-an-email", "password1")))
            .andExpect(status().isBadRequest());

        // then
        assertThat(memberCountByEmail("not-an-email")).isZero();
    }

    @Test
    @DisplayName("로그인은 기존 회원이면 서비스 토큰을 반환한다")
    void loginReturnsTokenForExistingMember() throws Exception {
        // when
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

        // then
        assertThat(jwtProvider.getEmail(token)).isEqualTo("user1@example.com");
    }

    @Test
    @DisplayName("로그인은 비밀번호가 틀리면 400을 반환한다")
    void loginRejectsWrongPassword() throws Exception {
        // when & then
        mockMvc.perform(post("/api/members/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(memberJson("user1@example.com", "wrong-password")))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("회원 이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("카카오 로그인은 카카오 인가 URL로 리다이렉트한다")
    void kakaoLoginRedirectsToKakaoAuthorizationUrl() throws Exception {
        // when & then
        mockMvc.perform(get("/api/auth/kakao/login"))
            .andExpect(status().isFound())
            .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString(
                "https://kauth.kakao.com/oauth/authorize"
            )))
            .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString("response_type=code")))
            .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString("scope=account_email,talk_message")));
    }

    @Test
    @DisplayName("카카오 콜백은 회원을 생성하고 카카오 접근 토큰 갱신 후 서비스 토큰을 반환한다")
    void kakaoCallbackCreatesMemberUpdatesAccessTokenAndReturnsServiceToken() throws Exception {
        // given
        when(kakaoLoginClient.requestAccessToken(eq("auth-code")))
            .thenReturn(new KakaoLoginClient.KakaoTokenResponse("kakao-access-token"));
        when(kakaoLoginClient.requestUserInfo(eq("kakao-access-token")))
            .thenReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("kakao-user@example.com")
            ));

        // when
        String token = mockMvc.perform(get("/api/auth/kakao/callback").param("code", "auth-code"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replace("{\"token\":\"", "")
            .replace("\"}", "");

        // then
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
