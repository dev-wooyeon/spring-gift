package gift.auth.support;

import gift.member.domain.Member;
import gift.member.application.MemberService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated member from an Authorization header.
 *
 * @author brian.kim
 * @since 1.0
 */
@Slf4j
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
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            log.error("Internal error during authentication resolver", e);
            throw new RuntimeException("Authentication internal error", e);
        }
    }
}

