package uk.co.pactlab.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record RegisterUserResponse(
        UUID id,
        String username,
        Instant createdAt,
        Instant updatedAt
) {
}
