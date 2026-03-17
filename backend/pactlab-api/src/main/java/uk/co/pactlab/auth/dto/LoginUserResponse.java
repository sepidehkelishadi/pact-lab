package uk.co.pactlab.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record LoginUserResponse(
        UUID id,
        String username,
        String token,
        Instant createdAt,
        Instant updatedAt
) {
}
