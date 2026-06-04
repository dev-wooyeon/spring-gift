package gift.member.application;

public interface MemberTokenProvider {
    String createToken(String email);
}
