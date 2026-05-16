package gift.member;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public TokenResponse register(MemberRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        Member member = memberRepository.save(new Member(request.email(), request.password()));
        return createToken(member);
    }

    public TokenResponse login(MemberRequest request) {
        Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (member.getPassword() == null || !member.getPassword().equals(request.password())) {
            throw new IllegalArgumentException("Invalid email or password.");
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
            .orElseThrow(() -> new IllegalArgumentException("Member not found. id=" + id));
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
    public void deleteMember(Long id) {
        memberRepository.deleteById(id);
    }

    @Transactional
    public TokenResponse updateKakaoAccessTokenAndIssueToken(String email, String accessToken) {
        Member member = memberRepository.findByEmail(email)
            .orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(accessToken);
        memberRepository.save(member);
        return createToken(member);
    }

    private TokenResponse createToken(Member member) {
        return new TokenResponse(jwtProvider.createToken(member.getEmail()));
    }
}
