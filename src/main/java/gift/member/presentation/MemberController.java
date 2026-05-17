package gift.member.presentation;

import gift.auth.presentation.TokenResponse;
import gift.member.application.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles member registration and login.
 *
 * @author brian.kim
 * @since 1.0
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody MemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new TokenResponse(memberService.register(request.toCommand())));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody MemberRequest request) {
        return ResponseEntity.ok(new TokenResponse(memberService.login(request.toCommand())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
