package gift.auth.support;

import gift.member.domain.Member;
import gift.member.application.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated member from an Authorization header.
 *
 * @author brian.kim
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class AuthenticationResolver {
    private final JwtProvider jwtProvider;
    private final MemberService memberService;

    public Member extractMember(String authorization) {
        try {
            final String token = authorization.replace("Bearer ", "");
            final String email = jwtProvider.getEmail(token);
            return memberService.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
