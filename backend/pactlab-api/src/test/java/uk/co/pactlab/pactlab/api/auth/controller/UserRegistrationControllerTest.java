package uk.co.pactlab.pactlab.api.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.co.pactlab.pactlab.api.auth.dto.RegisterUserRequest;
import uk.co.pactlab.pactlab.api.auth.dto.RegisterUserResponse;
import uk.co.pactlab.pactlab.api.auth.exception.UserAlreadyExistsException;
import uk.co.pactlab.pactlab.api.auth.service.UserRegistrationService;
import uk.co.pactlab.pactlab.api.shared.exception.GlobalExceptionHandler;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserRegistrationControllerTest {

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
        UserRegistrationService userRegistrationService = new UserRegistrationService(null, null) {
            @Override
            public RegisterUserResponse register(RegisterUserRequest request) {
                return new RegisterUserResponse(
                        UUID.fromString("1e59c4b6-2f23-49b7-a923-6a9c661afdb9"),
                        "sepideh@example.com",
                        Instant.parse("2026-03-14T11:00:00Z"),
                        Instant.parse("2026-03-14T11:00:00Z")
                );
            }
        };
        mockMvc = buildMockMvc(userRegistrationService);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterUserRequest("sepideh@example.com", "SecretPass1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1e59c4b6-2f23-49b7-a923-6a9c661afdb9"))
                .andExpect(jsonPath("$.username").value("sepideh@example.com"))
                .andExpect(jsonPath("$.createdAt").value("2026-03-14T11:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-03-14T11:00:00Z"));
    }

    @Test
    void returnsValidationErrorsForInvalidRequest() throws Exception {
        mockMvc = buildMockMvc(new UserRegistrationService(null, null));

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
        UserRegistrationService userRegistrationService = new UserRegistrationService(null, null) {
            @Override
            public RegisterUserResponse register(RegisterUserRequest request) {
                throw new UserAlreadyExistsException("sepideh@example.com");
            }
        };
        mockMvc = buildMockMvc(userRegistrationService);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterUserRequest("sepideh@example.com", "SecretPass1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User already exists for email: sepideh@example.com"));
    }

    private MockMvc buildMockMvc(UserRegistrationService userRegistrationService) {
        return MockMvcBuilders.standaloneSetup(new UserRegistrationController(userRegistrationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }
}
