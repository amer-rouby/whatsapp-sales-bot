package com.salesbot.identity.application;

import com.salesbot.identity.domain.User;
import com.salesbot.identity.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    /** Same message for unknown email and wrong password: never reveal which emails exist. */
    static final String BAD_CREDENTIALS = "Invalid email or password";
    /** A real hash to compare against when the email is unknown, so timing doesn't reveal it either. */
    private final String dummyHash;

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final LoginAttempts attempts;

    AuthService(UserRepository users, PasswordEncoder passwords, TokenService tokens, LoginAttempts attempts) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.attempts = attempts;
        this.dummyHash = passwords.encode("not-a-real-password");
    }

    @Transactional(readOnly = true)
    public LoginResult login(String rawEmail, String password) {
        String email = User.normalizeEmail(rawEmail);
        if (attempts.isBlocked(email)) {
            log.warn("Login blocked after repeated failures for {}", email);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Try again in a few minutes.");
        }

        User user = users.findByEmail(email).orElse(null);
        boolean ok = passwords.matches(password, user != null ? user.getPasswordHash() : dummyHash) && user != null;
        if (!ok) {
            attempts.recordFailure(email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }

        attempts.reset(email);
        return new LoginResult(tokens.issue(user), user);
    }

    public record LoginResult(TokenService.IssuedToken token, User user) {
    }
}
