package gift.order.application;

import gift.member.domain.Member;
import gift.member.infrastructure.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@SpringBootTest
class OrderTransactionTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private OrderOptionPort optionPort;

    @MockitoBean
    private OrderMemberPort memberPort;

    @Test
    @DisplayName("Checked Exception 발생 시 트랜잭션이 롤백되어 트랜잭션 내 저장된 회원 데이터가 취소된다")
    void transactionRollsBackOnCheckedExceptionAndEnlistedWriteIsRolledBack() throws Exception {
        // given
        String testEmail = "test-rollback@example.com";
        memberRepository.findByEmail(testEmail).ifPresent(memberRepository::delete);

        OrderMember member = new OrderMember(2L, "kakao-token");
        OrderCommand command = new OrderCommand(1L, 1, "선물 메시지");
        ReservedOption option = new ReservedOption(1L, "스페이스 블랙 / M1 Pro", 1L, "맥북 프로 16인치", 3_360_000);

        when(optionPort.reserveOption(1L, 1)).thenReturn(Optional.of(option));

        // memberPort.deductPoint() 실행 도중 실제 DB 쓰기(memberRepository.save)를 발생시킨 후 Checked Exception을 던진다.
        doAnswer(invocation -> {
            memberRepository.save(new Member(testEmail, "password"));
            throw new IOException("Checked Exception 발생");
        }).when(memberPort).deductPoint(anyLong(), anyInt());

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(member, command))
            .isInstanceOf(Exception.class)
            .hasRootCauseInstanceOf(IOException.class)
            .hasRootCauseMessage("Checked Exception 발생");

        // then: Checked Exception으로 인해 트랜잭션이 롤백되어 저장했던 회원 데이터가 존재하지 않아야 한다.
        assertThat(memberRepository.findByEmail(testEmail)).isEmpty();
    }
}
