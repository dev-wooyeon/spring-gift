package gift.auth.application;

public interface TokenProvider {
    String getEmail(String token);
    String createToken(String email);
}
