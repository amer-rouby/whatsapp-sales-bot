package com.salesbot.identity.infrastructure;

import com.salesbot.identity.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** @param email already normalized (see {@link User#normalizeEmail}) */
    Optional<User> findByEmail(String email);
}
