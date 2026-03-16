package uk.co.pactlab.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginUserRequest(
        @NotBlank
        @Email
        String username,
        @NotBlank
        String password
) {
}
