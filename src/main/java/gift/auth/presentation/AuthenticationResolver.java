package gift.auth.presentation;

import gift.auth.application.TokenProvider;
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
    private final TokenProvider tokenProvider;
    private final MemberService memberService;

    public Member extractMember(String authorization) {
        try {
            final String token = authorization.replace("Bearer ", "");
            final String email = tokenProvider.getEmail(token);
            return memberService.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
