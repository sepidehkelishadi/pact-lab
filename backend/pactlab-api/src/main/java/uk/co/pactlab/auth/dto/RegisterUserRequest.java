package uk.co.pactlab.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank
        @Email
        String username,
        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}
