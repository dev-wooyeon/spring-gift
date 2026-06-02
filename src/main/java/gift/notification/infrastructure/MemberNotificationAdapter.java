package gift.notification.infrastructure;

import gift.member.application.MemberService;
import gift.member.domain.Member;
import gift.notification.application.NotificationMemberPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberNotificationAdapter implements NotificationMemberPort {
    private final MemberService memberService;

    @Override
    public Optional<String> getKakaoAccessToken(Long memberId) {
        try {
            Member member = memberService.getMember(memberId);
            return Optional.ofNullable(member.getKakaoAccessToken());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
