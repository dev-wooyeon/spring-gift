package gift.order.infrastructure;

import gift.member.application.MemberService;
import gift.order.application.OrderMemberPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberOrderAdapter implements OrderMemberPort {
    private final MemberService memberService;

    @Override
    public void deductPoint(Long memberId, int amount) {
        memberService.deductPoint(memberId, amount);
    }
}
