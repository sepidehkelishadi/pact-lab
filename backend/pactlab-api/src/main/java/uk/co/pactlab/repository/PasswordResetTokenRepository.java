package uk.co.pactlab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.pactlab.entity.PasswordResetToken;
import uk.co.pactlab.entity.User;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(User user);

    long deleteByExpiresAtBefore(LocalDateTime time);
}