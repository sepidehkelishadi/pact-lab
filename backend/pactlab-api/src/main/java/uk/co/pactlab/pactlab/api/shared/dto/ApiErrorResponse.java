package uk.co.pactlab.pactlab.api.shared.dto;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String message,
        Instant timestamp,
        Map<String, String> fieldErrors
) {
}
