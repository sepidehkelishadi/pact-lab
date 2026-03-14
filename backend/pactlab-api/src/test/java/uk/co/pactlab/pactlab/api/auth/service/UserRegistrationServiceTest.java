package uk.co.pactlab.pactlab.api.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.co.pactlab.pactlab.api.auth.dto.RegisterUserRequest;
import uk.co.pactlab.pactlab.api.auth.dto.RegisterUserResponse;
import uk.co.pactlab.pactlab.api.auth.exception.UserAlreadyExistsException;
import uk.co.pactlab.pactlab.api.auth.model.User;
import uk.co.pactlab.pactlab.api.auth.repository.UserRepository;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserRegistrationServiceTest {

    @Test
    void registersUserWithNormalizedEmailAndHashedPassword() {
        RegisterUserRequest request = new RegisterUserRequest("  Test.User@Example.com ", "SecretPass1");
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-03-14T10:15:30Z");
        Instant updatedAt = Instant.parse("2026-03-14T10:15:30Z");
        AtomicReference<User> savedUserRef = new AtomicReference<>();

        UserRepository userRepository = repository(false, savedUserRef, userId, createdAt, updatedAt);
        UserRegistrationService userRegistrationService = new UserRegistrationService(userRepository, passwordEncoder());

        RegisterUserResponse response = userRegistrationService.register(request);

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
    void rejectsDuplicateEmail() {
        RegisterUserRequest request = new RegisterUserRequest("test.user@example.com", "SecretPass1");
        Instant now = Instant.now();
        UserRepository userRepository = repository(true, new AtomicReference<>(), UUID.randomUUID(), now, now);
        UserRegistrationService userRegistrationService = new UserRegistrationService(userRepository, passwordEncoder());

        assertThrows(UserAlreadyExistsException.class, () -> userRegistrationService.register(request));
    }

    private UserRepository repository(boolean exists, AtomicReference<User> savedUserRef, UUID userId, Instant createdAt, Instant updatedAt) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "existsByEmailIgnoreCase":
                    return exists;
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

    private void setField(User user, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = User.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(user, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "hashed-password";
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return false;
            }
        };
    }
}
