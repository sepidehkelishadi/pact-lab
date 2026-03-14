package uk.co.pactlab.pactlab.api.auth.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.pactlab.pactlab.api.auth.dto.RegisterUserRequest;
import uk.co.pactlab.pactlab.api.auth.dto.RegisterUserResponse;
import uk.co.pactlab.pactlab.api.auth.exception.UserAlreadyExistsException;
import uk.co.pactlab.pactlab.api.auth.model.User;
import uk.co.pactlab.pactlab.api.auth.repository.UserRepository;

import java.util.Locale;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request) {
        String normalizedEmail = request.username().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new UserAlreadyExistsException(normalizedEmail);
        }

        User user = new User();
        user.setEmail(normalizedEmail);
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
            throw new UserAlreadyExistsException(normalizedEmail);
        }
    }
}
