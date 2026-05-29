package gift.auth.application;

import gift.auth.infrastructure.KakaoLoginClient;
import gift.member.application.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {
    private final KakaoLoginClient kakaoLoginClient;
    private final MemberService memberService;

    public String getAuthorizationUrl() {
        return kakaoLoginClient.getAuthorizationUrl();
    }

    public String callback(String code) {
        KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
        KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(kakaoToken.accessToken());
        return memberService.updateKakaoAccessTokenAndIssueToken(kakaoUser.email(), kakaoToken.accessToken());
    }
}
