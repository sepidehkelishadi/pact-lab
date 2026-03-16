package uk.co.pactlab.auth.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.pactlab.auth.dto.LoginUserRequest;
import uk.co.pactlab.auth.dto.LoginUserResponse;
import uk.co.pactlab.auth.dto.RegisterUserRequest;
import uk.co.pactlab.auth.dto.RegisterUserResponse;
import uk.co.pactlab.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        RegisterUserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginUserResponse> login(@Valid @RequestBody LoginUserRequest request) {
        LoginUserResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}