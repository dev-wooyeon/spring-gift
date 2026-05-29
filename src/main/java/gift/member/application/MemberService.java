package gift.member.application;

import gift.auth.support.JwtProvider;
import gift.member.domain.Member;
import gift.member.exception.MemberException;
import gift.member.infrastructure.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public String register(MemberCommand command) {
        log.info("[MemberService] 회원 가입 개시 - 이메일: {}", command.email());
        if (memberRepository.existsByEmail(command.email())) {
            log.error("[MemberService] 회원 가입 실패 - 이미 존재하는 이메일: {}", command.email());
            throw MemberException.invalid("회원 이메일이 이미 등록되어 있습니다.");
        }

        Member member = memberRepository.save(new Member(command.email(), command.password()));
        log.info("[MemberService] 회원 가입 완료 - 회원 ID: {}, 이메일: {}", member.getId(), command.email());
        return createToken(member);
    }

    public String login(MemberCommand command) {
        log.info("[MemberService] 로그인 요청 - 이메일: {}", command.email());
        Member member = memberRepository.findByEmail(command.email())
            .orElseThrow(() -> {
                log.error("[MemberService] 로그인 실패 - 이메일 없음: {}", command.email());
                return MemberException.invalid("회원 이메일 또는 비밀번호가 올바르지 않습니다.");
            });

        if (member.getPassword() == null || !member.getPassword().equals(command.password())) {
            log.error("[MemberService] 로그인 실패 - 비밀번호 불일치: {}", command.email());
            throw MemberException.invalid("회원 이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        log.info("[MemberService] 로그인 성공 - 회원 ID: {}, 이메일: {}", member.getId(), command.email());
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
        log.info("[MemberService] 어드민 회원 강제 생성 - 이메일: {}", email);
        return memberRepository.save(new Member(email, password));
    }

    public Member getMember(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> {
                log.error("[MemberService] 회원 조회 실패 - 존재하지 않는 회원 ID: {}", id);
                return MemberException.notFound("회원을 찾을 수 없습니다. id=" + id);
            });
    }

    @Transactional
    public Member updateAdminMember(Long id, String email, String password) {
        log.info("[MemberService] 어드민 회원 수정 - 회원 ID: {}, 새 이메일: {}", id, email);
        Member member = getMember(id);
        member.update(email, password);
        return memberRepository.save(member);
    }

    @Transactional
    public Member chargePoint(Long id, int amount) {
        log.info("[MemberService] 회원 포인트 충전 요청 - 회원 ID: {}, 금액: {}", id, amount);
        Member member = getMember(id);
        member.chargePoint(amount);
        Member saved = memberRepository.save(member);
        log.info("[MemberService] 회원 포인트 충전 성공 - 회원 ID: {}, 현재 잔액: {}", id, saved.getPoint());
        return saved;
    }

    @Transactional
    public Member deductPoint(Long memberId, int amount) {
        log.info("[MemberService] 회원 포인트 차감 요청 - 회원 ID: {}, 금액: {}", memberId, amount);
        Member member = getMember(memberId);
        member.deductPoint(amount);
        Member saved = memberRepository.save(member);
        log.info("[MemberService] 회원 포인트 차감 성공 - 회원 ID: {}, 현재 잔액: {}", memberId, saved.getPoint());
        return saved;
    }

    @Transactional
    public void deleteMember(Long id) {
        log.info("[MemberService] 회원 삭제 요청 - 회원 ID: {}", id);
        memberRepository.deleteById(id);
    }

    @Transactional
    public String updateKakaoAccessTokenAndIssueToken(String email, String accessToken) {
        log.info("[MemberService] 카카오 로그인 연동 요청 - 이메일: {}", email);
        Member member = memberRepository.findByEmail(email)
            .orElseGet(() -> {
                log.info("[MemberService] 카카오 신규 회원 가입 - 이메일: {}", email);
                return new Member(email);
            });
        member.updateKakaoAccessToken(accessToken);
        memberRepository.save(member);
        log.info("[MemberService] 카카오 로그인 연동 성공 - 회원 ID: {}", member.getId());
        return createToken(member);
    }

    private String createToken(Member member) {
        return jwtProvider.createToken(member.getEmail());
    }
}
