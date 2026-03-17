package uk.co.pactlab.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.co.pactlab.auth.dto.ForgotPasswordRequest;
import uk.co.pactlab.auth.dto.ForgotPasswordResponse;
import uk.co.pactlab.auth.dto.LoginUserRequest;
import uk.co.pactlab.auth.dto.LoginUserResponse;
import uk.co.pactlab.auth.dto.RegisterUserRequest;
import uk.co.pactlab.auth.dto.RegisterUserResponse;
import uk.co.pactlab.auth.dto.ResetPasswordRequest;
import uk.co.pactlab.auth.dto.ResetPasswordResponse;
import uk.co.pactlab.auth.exception.EmailAlreadyExistsException;
import uk.co.pactlab.auth.exception.InvalidCredentialsException;
import uk.co.pactlab.auth.exception.InvalidPasswordResetTokenException;
import uk.co.pactlab.entity.PasswordResetToken;
import uk.co.pactlab.entity.User;
import uk.co.pactlab.repository.PasswordResetTokenRepository;
import uk.co.pactlab.repository.UserRepository;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    @Test
    void registersUserWithNormalizedEmailAndHashedPassword() {
        RegisterUserRequest request = new RegisterUserRequest("  Test.User@Example.com ", "SecretPass1");
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-03-16T10:15:30Z");
        Instant updatedAt = Instant.parse("2026-03-16T10:15:30Z");
        AtomicReference<User> savedUserRef = new AtomicReference<>();

        UserRepository userRepository = userRepository(false, null, savedUserRef, userId, createdAt, updatedAt);
        AuthService authService = authService(userRepository, passwordEncoder(true), passwordResetTokenRepository(null), emailService(), passwordResetService(null));

        RegisterUserResponse response = authService.register(request);

        User savedUser = savedUserRef.get();
        assertEquals("test.user@example.com", savedUser.getEmail());
        assertEquals("hashed-password", savedUser.getPasswordHash());
        assertNotEquals("SecretPass1", savedUser.getPasswordHash());
        assertEquals(userId, response.id());
        assertEquals("test.user@example.com", response.username());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void rejectsDuplicateEmailOnRegister() {
        RegisterUserRequest request = new RegisterUserRequest("test.user@example.com", "SecretPass1");
        Instant now = Instant.now();
        UserRepository userRepository = userRepository(true, null, new AtomicReference<>(), UUID.randomUUID(), now, now);
        AuthService authService = authService(userRepository, passwordEncoder(true), passwordResetTokenRepository(null), emailService(), passwordResetService(null));

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    void logsInUserWhenCredentialsMatch() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-03-16T09:00:00Z");
        Instant updatedAt = Instant.parse("2026-03-16T09:00:00Z");
        User user = user(userId, "john@example.com", "hashed-password", createdAt, updatedAt);

        UserRepository userRepository = userRepository(false, user, new AtomicReference<>(), userId, createdAt, updatedAt);
        AuthService authService = authService(userRepository, passwordEncoder(true), passwordResetTokenRepository(null), emailService(), passwordResetService(null));

        LoginUserResponse response = authService.login(new LoginUserRequest("John@Example.com", "SecretPass1"));

        assertEquals(userId, response.id());
        assertEquals("john@example.com", response.username());
        assertEquals("jwt-token", response.token());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void rejectsUnknownUserOnLogin() {
        UserRepository userRepository = userRepository(false, null, new AtomicReference<>(), UUID.randomUUID(), Instant.now(), Instant.now());
        AuthService authService = authService(userRepository, passwordEncoder(false), passwordResetTokenRepository(null), emailService(), passwordResetService(null));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginUserRequest("john@example.com", "SecretPass1")));
    }

    @Test
    void rejectsWrongPasswordOnLogin() {
        User user = user(UUID.randomUUID(), "john@example.com", "hashed-password", Instant.now(), Instant.now());
        UserRepository userRepository = userRepository(false, user, new AtomicReference<>(), UUID.randomUUID(), Instant.now(), Instant.now());
        AuthService authService = authService(userRepository, passwordEncoder(false), passwordResetTokenRepository(null), emailService(), passwordResetService(null));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginUserRequest("john@example.com", "WrongPass1")));
    }

    @Test
    void forgotPasswordReturnsGenericMessageAndSendsEmailForExistingUser() {
        User user = user(UUID.randomUUID(), "john@example.com", "hashed-password", Instant.now(), Instant.now());
        UserRepository userRepository = userRepository(false, user, new AtomicReference<>(), UUID.randomUUID(), Instant.now(), Instant.now());
        AtomicReference<String> sentEmail = new AtomicReference<>();
        AtomicReference<String> sentLink = new AtomicReference<>();
        PasswordResetToken resetToken = resetToken(user, "reset-token", false, LocalDateTime.now().plusMinutes(30));

        AuthService authService = authService(
                userRepository,
                passwordEncoder(true),
                passwordResetTokenRepository(resetToken),
                emailService(sentEmail, sentLink),
                passwordResetService(resetToken)
        );

        ForgotPasswordResponse response = authService.forgotPassword(new ForgotPasswordRequest("john@example.com"));

        assertEquals("If an account with that email exists, a password reset link has been sent.", response.message());
        assertEquals("john@example.com", sentEmail.get());
        assertTrue(sentLink.get().endsWith("reset-token"));
    }

    @Test
    void resetPasswordUpdatesHashAndMarksTokenUsed() {
        User user = user(UUID.randomUUID(), "john@example.com", "old-hash", Instant.now(), Instant.now());
        PasswordResetToken resetToken = resetToken(user, "reset-token", false, LocalDateTime.now().plusMinutes(30));
        AtomicReference<User> savedUserRef = new AtomicReference<>();
        AtomicReference<PasswordResetToken> savedTokenRef = new AtomicReference<>();

        UserRepository userRepository = userRepository(false, null, savedUserRef, UUID.randomUUID(), Instant.now(), Instant.now());
        AuthService authService = authService(
                userRepository,
                passwordEncoder(true),
                passwordResetTokenRepository(resetToken, savedTokenRef),
                emailService(),
                passwordResetService(resetToken)
        );

        ResetPasswordResponse response = authService.resetPassword(
                new ResetPasswordRequest("reset-token", "NewSecret1", "NewSecret1")
        );

        assertEquals("Password reset successfully", response.message());
        assertEquals("hashed-password", savedUserRef.get().getPasswordHash());
        assertTrue(savedTokenRef.get().isUsed());
    }

    @Test
    void resetPasswordRejectsInvalidToken() {
        UserRepository userRepository = userRepository(false, null, new AtomicReference<>(), UUID.randomUUID(), Instant.now(), Instant.now());
        AuthService authService = authService(
                userRepository,
                passwordEncoder(true),
                passwordResetTokenRepository(null),
                emailService(),
                passwordResetService(null)
        );

        assertThrows(InvalidPasswordResetTokenException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("missing", "NewSecret1", "NewSecret1")));
    }

    private AuthService authService(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    PasswordResetTokenRepository passwordResetTokenRepository,
                                    EmailService emailService,
                                    PasswordResetService passwordResetService) {
        return new AuthService(
                userRepository,
                passwordEncoder,
                jwtService(),
                passwordResetService,
                passwordResetTokenRepository,
                emailService,
                "https://example.com/reset?token="
        );
    }

    private UserRepository userRepository(boolean exists, User foundUser, AtomicReference<User> savedUserRef, UUID userId, Instant createdAt, Instant updatedAt) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "existsByEmailIgnoreCase":
                    return exists;
                case "findByEmailIgnoreCase":
                    return Optional.ofNullable(foundUser);
                case "save":
                    User user = (User) args[0];
                    savedUserRef.set(user);
                    setField(user, "id", userId);
                    setField(user, "createdAt", createdAt);
                    setField(user, "updatedAt", updatedAt);
                    return user;
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return "UserRepositoryStub";
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        };

        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class[]{UserRepository.class},
                handler
        );
    }

    private PasswordResetTokenRepository passwordResetTokenRepository(PasswordResetToken resetToken) {
        return passwordResetTokenRepository(resetToken, new AtomicReference<>());
    }

    private PasswordResetTokenRepository passwordResetTokenRepository(PasswordResetToken resetToken, AtomicReference<PasswordResetToken> savedTokenRef) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "findByToken":
                    return Optional.ofNullable(resetToken);
                case "save":
                    PasswordResetToken token = (PasswordResetToken) args[0];
                    savedTokenRef.set(token);
                    return token;
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return "PasswordResetTokenRepositoryStub";
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        };

        return (PasswordResetTokenRepository) Proxy.newProxyInstance(
                PasswordResetTokenRepository.class.getClassLoader(),
                new Class[]{PasswordResetTokenRepository.class},
                handler
        );
    }

    private PasswordEncoder passwordEncoder(boolean matches) {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "hashed-password";
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return matches;
            }
        };
    }

    private JwtService jwtService() {
        return new JwtService("12345678901234567890123456789012", 86400000L) {
            @Override
            public String generateToken(UUID userId, String email) {
                return "jwt-token";
            }
        };
    }

    private PasswordResetService passwordResetService(PasswordResetToken resetToken) {
        return new PasswordResetService(null, 30) {
            @Override
            public PasswordResetToken createTokenForUser(User user) {
                return resetToken;
            }
        };
    }

    private EmailService emailService() {
        return emailService(new AtomicReference<>(), new AtomicReference<>());
    }

    private EmailService emailService(AtomicReference<String> sentEmail, AtomicReference<String> sentLink) {
        return new EmailService(null) {
            @Override
            public void sendPasswordResetEmail(String toEmail, String resetLink) {
                sentEmail.set(toEmail);
                sentLink.set(resetLink);
            }
        };
    }

    private User user(UUID id, String email, String passwordHash, Instant createdAt, Instant updatedAt) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        setField(user, "id", id);
        setField(user, "createdAt", createdAt);
        setField(user, "updatedAt", updatedAt);
        return user;
    }

    private PasswordResetToken resetToken(User user, String token, boolean used, LocalDateTime expiresAt) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setUsed(used);
        resetToken.setExpiresAt(expiresAt);
        return resetToken;
    }

    private void setField(User user, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = User.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(user, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
