package uk.co.pactlab.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import uk.co.pactlab.auth.dto.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.pactlab.auth.exception.InvalidCredentialsException;
import uk.co.pactlab.auth.exception.EmailAlreadyExistsException;
import uk.co.pactlab.auth.exception.InvalidPasswordResetTokenException;
import uk.co.pactlab.entity.PasswordResetToken;
import uk.co.pactlab.entity.User;
import uk.co.pactlab.repository.PasswordResetTokenRepository;
import uk.co.pactlab.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final String resetUrlBase;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       PasswordResetService passwordResetService,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailService emailService,
                       @Value("${app.password-reset.reset-url-base}") String resetUrlBase) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.resetUrlBase = resetUrlBase;
    }

    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request) {
        String email = request.username().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        try {
            User savedUser = userRepository.save(user);
            return new RegisterUserResponse(
                    savedUser.getId(),
                    savedUser.getEmail(),
                    savedUser.getCreatedAt(),
                    savedUser.getUpdatedAt()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    @Transactional(readOnly = true)
    public LoginUserResponse login(LoginUserRequest request) {
        String email = request.username().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return new LoginUserResponse(
                user.getId(),
                user.getEmail(),
                token,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            PasswordResetToken token = passwordResetService.createTokenForUser(user);
            String resetLink = resetUrlBase + token.getToken();
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        });

        return new ForgotPasswordResponse(
                "If an account with that email exists, a password reset link has been sent."
        );
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid or expired reset token"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidPasswordResetTokenException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return new ResetPasswordResponse("Password reset successfully");
    }
}
