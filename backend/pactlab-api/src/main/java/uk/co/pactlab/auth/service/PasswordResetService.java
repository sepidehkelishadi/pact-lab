package uk.co.pactlab.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.co.pactlab.entity.PasswordResetToken;
import uk.co.pactlab.entity.User;
import uk.co.pactlab.repository.PasswordResetTokenRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final int tokenExpirationMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            PasswordResetTokenRepository passwordResetTokenRepository,
            @Value("${app.password-reset.token-expiration-minutes}") int tokenExpirationMinutes
    ) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenExpirationMinutes = tokenExpirationMinutes;
    }

    public PasswordResetToken createTokenForUser(User user) {
        passwordResetTokenRepository.deleteByUser(user);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(generateSecureToken());
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(tokenExpirationMinutes));
        resetToken.setUsed(false);

        return passwordResetTokenRepository.save(resetToken);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}