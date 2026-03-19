package uk.co.bisque.auth.model

data class LoginRequest(
    val username: String,
    val password: String
)