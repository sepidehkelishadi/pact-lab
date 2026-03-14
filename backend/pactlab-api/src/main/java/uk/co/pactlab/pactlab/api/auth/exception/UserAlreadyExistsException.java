package uk.co.pactlab.pactlab.api.auth.exception;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email) {
        super("User already exists for email: " + email);
    }
}
