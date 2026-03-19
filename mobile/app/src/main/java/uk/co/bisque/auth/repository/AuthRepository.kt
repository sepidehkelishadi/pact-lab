package uk.co.bisque.auth.repository

import uk.co.bisque.auth.model.LoginRequest
import uk.co.bisque.auth.model.LoginResponse
import uk.co.bisque.network.RetrofitClient


class AuthRepository {

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = RetrofitClient.authApiService.login(
                LoginRequest(username = username, password = password)
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}