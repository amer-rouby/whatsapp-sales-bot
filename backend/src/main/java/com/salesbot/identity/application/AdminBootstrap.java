package com.salesbot.identity.application;

import com.salesbot.identity.domain.User;
import com.salesbot.identity.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first owner account from {@code salesbot.admin.*} when there are no users yet.
 * Runs on every start but only acts once; the password is never logged.
 */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    static final int MIN_PASSWORD_LENGTH = 10;

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final String email;
    private final String name;
    private final String password;

    AdminBootstrap(UserRepository users, PasswordEncoder passwords,
                   @Value("${salesbot.admin.email:}") String email,
                   @Value("${salesbot.admin.name:}") String name,
                   @Value("${salesbot.admin.password:}") String password) {
        this.users = users;
        this.passwords = passwords;
        this.email = email;
        this.name = name;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            log.warn("No dashboard users yet. Set SALESBOT_ADMIN_EMAIL and SALESBOT_ADMIN_PASSWORD to create the first one.");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.error("SALESBOT_ADMIN_PASSWORD must be at least {} characters; first user NOT created.", MIN_PASSWORD_LENGTH);
            return;
        }
        String displayName = name.isBlank() ? email : name;
        users.save(new User(email, displayName, passwords.encode(password), User.Role.OWNER));
        log.info("Created the first dashboard user {}", User.normalizeEmail(email));
    }
}
