package uk.co.pactlab.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.co.pactlab.auth.dto.LoginUserRequest;
import uk.co.pactlab.auth.dto.LoginUserResponse;
import uk.co.pactlab.auth.dto.RegisterUserRequest;
import uk.co.pactlab.auth.dto.RegisterUserResponse;
import uk.co.pactlab.auth.exception.InvalidCredentialsException;
import uk.co.pactlab.auth.exception.EmailAlreadyExistsException;
import uk.co.pactlab.auth.service.AuthService;
import uk.co.pactlab.shared.exception.GlobalExceptionHandler;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void returnsCreatedUser() throws Exception {
        AuthService authService = new AuthService(null, null, null) {
            @Override
            public RegisterUserResponse register(RegisterUserRequest request) {
                return new RegisterUserResponse(
                        UUID.fromString("1e59c4b6-2f23-49b7-a923-6a9c661afdb9"),
                        "sepideh@example.com",
                        Instant.parse("2026-03-16T11:00:00Z"),
                        Instant.parse("2026-03-16T11:00:00Z")
                );
            }
        };
        mockMvc = buildMockMvc(authService);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterUserRequest("sepideh@example.com", "SecretPass1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1e59c4b6-2f23-49b7-a923-6a9c661afdb9"))
                .andExpect(jsonPath("$.username").value("sepideh@example.com"))
                .andExpect(jsonPath("$.createdAt").value("2026-03-16T11:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-03-16T11:00:00Z"));
    }

    @Test
    void returnsValidationErrorsForInvalidRegisterRequest() throws Exception {
        mockMvc = buildMockMvc(new AuthService(null, null, null));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterUserRequest("not-an-email", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void returnsConflictForDuplicateEmail() throws Exception {
        AuthService authService = new AuthService(null, null, null) {
            @Override
            public RegisterUserResponse register(RegisterUserRequest request) {
                throw new EmailAlreadyExistsException("sepideh@example.com");
            }
        };
        mockMvc = buildMockMvc(authService);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterUserRequest("sepideh@example.com", "SecretPass1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User already exists for email: sepideh@example.com"));
    }

    @Test
    void returnsLoggedInUserWithToken() throws Exception {
        AuthService authService = new AuthService(null, null, null) {
            @Override
            public LoginUserResponse login(LoginUserRequest request) {
                return new LoginUserResponse(
                        UUID.fromString("1e59c4b6-2f23-49b7-a923-6a9c661afdb9"),
                        "sepideh@example.com",
                        "jwt-token",
                        Instant.parse("2026-03-16T11:00:00Z"),
                        Instant.parse("2026-03-16T11:00:00Z")
                );
            }
        };
        mockMvc = buildMockMvc(authService);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginUserRequest("sepideh@example.com", "SecretPass1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1e59c4b6-2f23-49b7-a923-6a9c661afdb9"))
                .andExpect(jsonPath("$.username").value("sepideh@example.com"))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void returnsUnauthorizedForInvalidLogin() throws Exception {
        AuthService authService = new AuthService(null, null, null) {
            @Override
            public LoginUserResponse login(LoginUserRequest request) {
                throw new InvalidCredentialsException();
            }
        };
        mockMvc = buildMockMvc(authService);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginUserRequest("sepideh@example.com", "WrongPass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    private MockMvc buildMockMvc(AuthService authService) {
        return MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }
}
