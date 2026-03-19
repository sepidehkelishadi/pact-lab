package uk.co.bisque.storage

import android.content.Context

class TokenStorage(context: Context) {

    private val sharedPreferences =
        context.getSharedPreferences("pactlab_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        sharedPreferences.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString("jwt_token", null)
    }

    fun clearToken() {
        sharedPreferences.edit().remove("jwt_token").apply()
    }
}