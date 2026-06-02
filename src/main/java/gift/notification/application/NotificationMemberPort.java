package gift.notification.application;

import java.util.Optional;

public interface NotificationMemberPort {
    Optional<String> getKakaoAccessToken(Long memberId);
}
