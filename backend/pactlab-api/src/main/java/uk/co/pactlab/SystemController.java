package uk.co.pactlab;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class SystemController {

    @Value("${spring.application.name:pact-lab-backend}")
    private String appName;

    @Value("${app.version:dev}")
    private String appVersion;

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/version")
    public Map<String, Object> version() {
        return Map.of(
                "name", appName,
                "version", appVersion,
                "timestamp", Instant.now().toString()
        );
    }
}
