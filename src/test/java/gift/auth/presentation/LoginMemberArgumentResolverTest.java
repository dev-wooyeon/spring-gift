package gift.auth.presentation;

import gift.auth.exception.UnauthenticatedException;
import gift.member.domain.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginMemberArgumentResolverTest {

    @Mock
    private AuthenticationResolver authenticationResolver;

    @InjectMocks
    private LoginMemberArgumentResolver loginMemberArgumentResolver;

    private NativeWebRequest webRequest;

    @BeforeEach
    void setUp() {
        webRequest = mock(NativeWebRequest.class);
    }

    @Test
    @DisplayName("supportsParameter는 @LoginMember가 붙고 Member 타입인 경우 true를 반환한다")
    void supportsParameterReturnsTrueForAnnotatedMember() throws Exception {
        // given
        MethodParameter parameter = getMethodParameter("securedMethod", Member.class);

        // when
        boolean result = loginMemberArgumentResolver.supportsParameter(parameter);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("supportsParameter는 @LoginMember가 붙었으나 Member 타입이 아니면 false를 반환한다")
    void supportsParameterReturnsFalseForAnnotatedWrongType() throws Exception {
        // given
        MethodParameter parameter = getMethodParameter("wrongTypeMethod", String.class);

        // when
        boolean result = loginMemberArgumentResolver.supportsParameter(parameter);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("supportsParameter는 @LoginMember가 안 붙은 Member 타입이면 false를 반환한다")
    void supportsParameterReturnsFalseForUnannotatedMember() throws Exception {
        // given
        MethodParameter parameter = getMethodParameter("noAnnotationMethod", Member.class);

        // when
        boolean result = loginMemberArgumentResolver.supportsParameter(parameter);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("resolveArgument는 유효한 Authorization 헤더 토큰이 주어지면 Member를 반환한다")
    void resolveArgumentReturnsMemberWhenHeaderIsValid() throws Exception {
        // given
        MethodParameter parameter = getMethodParameter("securedMethod", Member.class);
        String token = "Bearer valid-jwt";
        Member member = new Member("member@example.com", "password");
        
        when(webRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(authenticationResolver.extractMember(token)).thenReturn(member);

        // when
        Object result = loginMemberArgumentResolver.resolveArgument(
            parameter, null, webRequest, null
        );

        // then
        assertThat(result).isEqualTo(member);
    }

    @Test
    @DisplayName("resolveArgument는 필수인 조건에서 Authorization 헤더가 누락되면 UnauthenticatedException을 던진다")
    void resolveArgumentThrowsExceptionWhenHeaderIsMissingAndRequired() throws Exception {
        // given
        MethodParameter parameter = getMethodParameter("securedMethod", Member.class);
        when(webRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> loginMemberArgumentResolver.resolveArgument(
            parameter, null, webRequest, null
        )).isInstanceOf(UnauthenticatedException.class)
          .hasMessageContaining("인증 정보가 유효하지 않거나 존재하지 않습니다.");
    }

    @Test
    @DisplayName("resolveArgument는 필수인 조건에서 토큰 파싱 결과가 null이면 UnauthenticatedException을 던진다")
    void resolveArgumentThrowsExceptionWhenMemberIsNullAndRequired() throws Exception {
        // given
        MethodParameter parameter = getMethodParameter("securedMethod", Member.class);
        String token = "Bearer invalid-jwt";
        
        when(webRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(authenticationResolver.extractMember(token)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> loginMemberArgumentResolver.resolveArgument(
            parameter, null, webRequest, null
        )).isInstanceOf(UnauthenticatedException.class)
          .hasMessageContaining("인증 정보가 유효하지 않거나 존재하지 않습니다.");
    }

    @Test
    @DisplayName("resolveArgument는 선택인 조건(required = false)에서 토큰 파싱 결과가 null이면 null을 반환한다")
    void resolveArgumentReturnsNullWhenMemberIsNullAndOptional() throws Exception {
        // given
        MethodParameter parameter = getMethodParameter("optionalMethod", Member.class);
        String token = "Bearer invalid-jwt";
        
        when(webRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(authenticationResolver.extractMember(token)).thenReturn(null);

        // when
        Object result = loginMemberArgumentResolver.resolveArgument(
            parameter, null, webRequest, null
        );

        // then
        assertThat(result).isNull();
    }

    private MethodParameter getMethodParameter(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = TestController.class.getDeclaredMethod(methodName, parameterTypes);
        return new MethodParameter(method, 0);
    }

    static class TestController {
        void securedMethod(@LoginMember Member member) {}
        void optionalMethod(@LoginMember(required = false) Member member) {}
        void noAnnotationMethod(Member member) {}
        void wrongTypeMethod(@LoginMember String wrongType) {}
    }
}
