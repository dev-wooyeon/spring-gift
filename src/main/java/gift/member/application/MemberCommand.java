package gift.member.application;

public record MemberCommand(
    String email,
    String password
) {
}
