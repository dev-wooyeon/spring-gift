package gift.auth.infrastructure;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Declarative HTTP Interface for Kakao OAuth2 APIs.
 * Utilizes Spring Boot 3.x Native HTTP Interface Support.
 *
 * @author brian.kim
 * @since 1.0
 */
@HttpExchange
public interface KakaoApi {

    /**
     * Request access token from Kakao OAuth2 Server.
     */
    @PostExchange(
        value = "https://kauth.kakao.com/oauth/token",
        contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    KakaoLoginClient.KakaoTokenResponse requestAccessToken(
        @RequestParam MultiValueMap<String, String> params
    );

    /**
     * Request user info from Kakao API Server using the access token.
     */
    @GetExchange("https://kapi.kakao.com/v2/user/me")
    KakaoLoginClient.KakaoUserResponse requestUserInfo(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    );
}
