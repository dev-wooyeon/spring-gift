package gift.auth.infrastructure;

import gift.auth.support.JwtProvider;
import gift.member.application.MemberTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtMemberTokenAdapter implements MemberTokenProvider {
    private final JwtProvider jwtProvider;

    @Override
    public String createToken(String email) {
        return jwtProvider.createToken(email);
    }
}
