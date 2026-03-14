package uk.co.pactlab.pactlab.api.auth.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.co.pactlab.pactlab.api.auth.dto.RegisterUserRequest;
import uk.co.pactlab.pactlab.api.auth.dto.RegisterUserResponse;
import uk.co.pactlab.pactlab.api.auth.service.UserRegistrationService;

@RestController
@RequestMapping("/api/auth")
public class UserRegistrationController {

    private final UserRegistrationService userRegistrationService;

    public UserRegistrationController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterUserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return userRegistrationService.register(request);
    }
}
