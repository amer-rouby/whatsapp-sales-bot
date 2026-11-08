package com.salesbot.identity.api;

import com.salesbot.identity.application.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthService auth;

    AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** Public: exchanges email + password for a signed token. */
    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = auth.login(request.email(), request.password());
        return new LoginResponse(result.token().token(), result.token().expiresAt(),
                new UserResponse(result.user().getEmail(), result.user().getName(), result.user().getRole().name()));
    }

    /** Who is signed in, straight from the validated token. */
    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new UserResponse(jwt.getClaimAsString("email"), jwt.getClaimAsString("name"), jwt.getClaimAsString("role"));
    }

    /** No @Email here: the address is trimmed/lower-cased first; a malformed one simply doesn't match a user. */
    record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 200) String password) {
    }

    record LoginResponse(String token, Instant expiresAt, UserResponse user) {
    }

    record UserResponse(String email, String name, String role) {
    }
}
