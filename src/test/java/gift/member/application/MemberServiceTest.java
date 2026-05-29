package gift.member.application;

import gift.auth.application.TokenProvider;
import gift.member.domain.Member;
import gift.member.exception.MemberException;
import gift.member.infrastructure.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원 가입은 중복 이메일이 아니면 회원을 저장하고 토큰을 발급한다")
    void registerSavesMemberAndReturnsToken() {
        // given
        MemberCommand command = new MemberCommand("member@example.com", "password");
        when(memberRepository.existsByEmail("member@example.com")).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.createToken("member@example.com")).thenReturn("service-token");

        // when
        String token = memberService.register(command);

        // then
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getEmail()).isEqualTo("member@example.com");
        assertThat(memberCaptor.getValue().getPassword()).isEqualTo("password");
        assertThat(memberCaptor.getValue().getPoint()).isZero();
        assertThat(token).isEqualTo("service-token");
    }

    @Test
    @DisplayName("회원 가입은 이미 등록된 이메일이면 회원을 저장하지 않는다")
    void registerRejectsDuplicateEmail() {
        // given
        MemberCommand command = new MemberCommand("member@example.com", "password");
        when(memberRepository.existsByEmail("member@example.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.register(command))
            .isInstanceOf(MemberException.class)
            .hasMessage("회원 이메일이 이미 등록되어 있습니다.");
        verify(memberRepository, never()).save(any());
        verify(tokenProvider, never()).createToken(anyString());
    }

    @Test
    @DisplayName("로그인은 이메일과 비밀번호가 일치하면 토큰을 발급한다")
    void loginReturnsTokenWhenCredentialsMatch() {
        // given
        MemberCommand command = new MemberCommand("member@example.com", "password");
        Member member = member(1L, "member@example.com", "password");
        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(tokenProvider.createToken("member@example.com")).thenReturn("service-token");

        // when
        String token = memberService.login(command);

        // then
        assertThat(token).isEqualTo("service-token");
        verify(tokenProvider).createToken("member@example.com");
    }

    @Test
    @DisplayName("로그인은 이메일이 없으면 토큰을 발급하지 않는다")
    void loginRejectsMissingEmail() {
        // given
        MemberCommand command = new MemberCommand("member@example.com", "password");
        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.login(command))
            .isInstanceOf(MemberException.class)
            .hasMessage("회원 이메일 또는 비밀번호가 올바르지 않습니다.");
        verify(tokenProvider, never()).createToken(anyString());
    }

    @Test
    @DisplayName("로그인은 비밀번호가 다르면 토큰을 발급하지 않는다")
    void loginRejectsWrongPassword() {
        // given
        MemberCommand command = new MemberCommand("member@example.com", "wrong-password");
        Member member = member(1L, "member@example.com", "password");
        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> memberService.login(command))
            .isInstanceOf(MemberException.class)
            .hasMessage("회원 이메일 또는 비밀번호가 올바르지 않습니다.");
        verify(tokenProvider, never()).createToken(anyString());
    }

    @Test
    @DisplayName("회원 조회는 ID에 해당하는 회원을 반환한다")
    void getMemberReturnsMember() {
        // given
        Member member = member(1L, "member@example.com", "password");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        // when
        Member result = memberService.getMember(1L);

        // then
        assertThat(result).isEqualTo(member);
    }

    @Test
    @DisplayName("회원 조회는 ID에 해당하는 회원이 없으면 예외가 발생한다")
    void getMemberRejectsMissingMember() {
        // given
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMember(1L))
            .isInstanceOf(MemberException.class)
            .hasMessage("회원을 찾을 수 없습니다. id=1");
    }

    @Test
    @DisplayName("포인트 충전은 회원을 조회한 뒤 포인트를 증가시키고 저장한다")
    void chargePointIncreasesMemberPointAndSavesMember() {
        // given
        Member member = member(1L, "member@example.com", "password");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.save(member)).thenReturn(member);

        // when
        Member result = memberService.chargePoint(1L, 10_000);

        // then
        assertThat(result.getPoint()).isEqualTo(10_000);
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("포인트 차감은 회원을 조회한 뒤 포인트를 감소시키고 저장한다")
    void deductPointDecreasesMemberPointAndSavesMember() {
        // given
        Member member = member(1L, "member@example.com", "password");
        member.chargePoint(10_000);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.save(member)).thenReturn(member);

        // when
        Member result = memberService.deductPoint(1L, 4_000);

        // then
        assertThat(result.getPoint()).isEqualTo(6_000);
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("카카오 로그인은 기존 회원의 카카오 접근 토큰을 갱신하고 서비스 토큰을 발급한다")
    void updateKakaoAccessTokenAndIssueTokenUpdatesExistingMember() {
        // given
        Member member = member(1L, "member@example.com", null);
        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(memberRepository.save(member)).thenReturn(member);
        when(tokenProvider.createToken("member@example.com")).thenReturn("service-token");

        // when
        String token = memberService.updateKakaoAccessTokenAndIssueToken("member@example.com", "kakao-token");

        // then
        assertThat(member.getKakaoAccessToken()).isEqualTo("kakao-token");
        assertThat(token).isEqualTo("service-token");
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("카카오 로그인은 신규 이메일이면 카카오 회원을 생성하고 서비스 토큰을 발급한다")
    void updateKakaoAccessTokenAndIssueTokenCreatesKakaoMember() {
        // given
        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.createToken("member@example.com")).thenReturn("service-token");

        // when
        String token = memberService.updateKakaoAccessTokenAndIssueToken("member@example.com", "kakao-token");

        // then
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getEmail()).isEqualTo("member@example.com");
        assertThat(memberCaptor.getValue().getPassword()).isNull();
        assertThat(memberCaptor.getValue().getKakaoAccessToken()).isEqualTo("kakao-token");
        assertThat(token).isEqualTo("service-token");
    }

    private Member member(Long id, String email, String password) {
        Member member = new Member(email, password);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
