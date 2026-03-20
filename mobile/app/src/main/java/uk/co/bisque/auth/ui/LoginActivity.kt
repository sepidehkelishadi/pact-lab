package uk.co.bisque.auth.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uk.co.bisque.R
import uk.co.bisque.auth.repository.AuthRepository
import uk.co.bisque.storage.TokenStorage


class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvMessage: TextView

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvMessage = findViewById(R.id.tvMessage)

        btnLogin.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            tvMessage.text = "Email and password are required"
            return
        }

        lifecycleScope.launch {
            btnLogin.isEnabled = false
            tvMessage.text = "Logging in..."

            val result = authRepository.login(email, password)

            btnLogin.isEnabled = true

            result.onSuccess { response ->
                TokenStorage(this@LoginActivity).saveToken(response.token)
                tvMessage.text = "Login successful"

                startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                finish()
            }.onFailure {
                tvMessage.text = it.message ?: "Login failed"
            }
        }
    }
}