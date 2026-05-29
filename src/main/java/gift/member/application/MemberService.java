package gift.member.application;

import gift.auth.support.JwtProvider;
import gift.member.domain.Member;
import gift.member.exception.MemberException;
import gift.member.infrastructure.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public String register(MemberCommand command) {
        if (memberRepository.existsByEmail(command.email())) {
            throw MemberException.invalid("회원 이메일이 이미 등록되어 있습니다.");
        }

        Member member = memberRepository.save(new Member(command.email(), command.password()));
        return createToken(member);
    }

    public String login(MemberCommand command) {
        Member member = memberRepository.findByEmail(command.email())
            .orElseThrow(() -> MemberException.invalid("회원 이메일 또는 비밀번호가 올바르지 않습니다."));

        if (member.getPassword() == null || !member.getPassword().equals(command.password())) {
            throw MemberException.invalid("회원 이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return createToken(member);
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public List<Member> getAdminMembers() {
        return memberRepository.findAll();
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Transactional
    public Member createAdminMember(String email, String password) {
        return memberRepository.save(new Member(email, password));
    }

    public Member getMember(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> MemberException.notFound("회원을 찾을 수 없습니다."));
    }

    @Transactional
    public Member updateAdminMember(Long id, String email, String password) {
        Member member = getMember(id);
        member.update(email, password);
        return memberRepository.save(member);
    }

    @Transactional
    public Member chargePoint(Long id, int amount) {
        Member member = getMember(id);
        member.chargePoint(amount);
        return memberRepository.save(member);
    }

    @Transactional
    public Member deductPoint(Long memberId, int amount) {
        Member member = getMember(memberId);
        member.deductPoint(amount);
        return memberRepository.save(member);
    }

    @Transactional
    public void deleteMember(Long id) {
        memberRepository.deleteById(id);
    }

    @Transactional
    public String updateKakaoAccessTokenAndIssueToken(String email, String accessToken) {
        Member member = memberRepository.findByEmail(email)
            .orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(accessToken);
        memberRepository.save(member);
        return createToken(member);
    }

    private String createToken(Member member) {
        return jwtProvider.createToken(member.getEmail());
    }
}
