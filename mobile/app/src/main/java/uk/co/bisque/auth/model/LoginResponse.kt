package uk.co.bisque.auth.model

data class LoginResponse(
    val userId: Long,
    val email: String,
    val token: String,
    val message: String
)