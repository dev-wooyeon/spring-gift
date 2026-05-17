package gift.order.application;

public interface OrderMemberPort {
    void deductPoint(Long memberId, int amount);
}
