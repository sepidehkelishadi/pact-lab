package uk.co.bisque.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import uk.co.bisque.auth.model.LoginRequest
import uk.co.bisque.auth.model.LoginResponse

interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}