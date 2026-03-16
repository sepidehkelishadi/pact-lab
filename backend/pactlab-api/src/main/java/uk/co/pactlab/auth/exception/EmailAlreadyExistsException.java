package uk.co.pactlab.auth.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("User already exists for email: " + email);
    }
}
