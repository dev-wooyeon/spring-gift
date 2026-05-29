package gift.auth.presentation;

import gift.auth.exception.UnauthenticatedException;
import gift.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves the authenticated Member domain object from the Authorization header
 * for controller method parameters annotated with @LoginMember.
 *
 * @author brian.kim
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final AuthenticationResolver authenticationResolver;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class)
            && Member.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        LoginMember loginMember = parameter.getParameterAnnotation(LoginMember.class);
        if (loginMember == null) {
            return null;
        }

        String authorization = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
        Member member = null;

        if (authorization != null && !authorization.isBlank()) {
            member = authenticationResolver.extractMember(authorization);
        }

        if (loginMember.required() && member == null) {
            throw new UnauthenticatedException("인증 정보가 유효하지 않거나 존재하지 않습니다.");
        }

        return member;
    }
}
