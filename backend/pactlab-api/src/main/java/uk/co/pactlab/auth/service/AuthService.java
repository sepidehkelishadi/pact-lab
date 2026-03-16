package uk.co.pactlab.auth.service;

import org.springframework.dao.DataIntegrityViolationException;
import uk.co.pactlab.auth.dto.LoginUserRequest;
import uk.co.pactlab.auth.dto.LoginUserResponse;
import uk.co.pactlab.auth.dto.RegisterUserRequest;
import uk.co.pactlab.auth.dto.RegisterUserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.pactlab.auth.exception.InvalidCredentialsException;
import uk.co.pactlab.auth.exception.EmailAlreadyExistsException;
import uk.co.pactlab.entity.User;
import uk.co.pactlab.repository.UserRepository;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
}
