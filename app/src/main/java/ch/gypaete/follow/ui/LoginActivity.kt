package ch.gypaete.follow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ch.gypaete.follow.R
import ch.gypaete.follow.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val prefs      = getSharedPreferences("follow", Context.MODE_PRIVATE)
        val etUrl      = findViewById<EditText>(R.id.etUrl)
        val etEmail    = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin   = findViewById<Button>(R.id.btnSave)
        val tvError    = findViewById<TextView>(R.id.tvError)
        val progress   = findViewById<ProgressBar>(R.id.progressLogin)

        etUrl.setText(prefs.getString("base_url", "https://"))
        etEmail.setText(prefs.getString("last_email", ""))

        btnLogin.setOnClickListener {
            val url = etUrl.text.toString().trim().let {
                if (it.endsWith("/")) it else "$it/"
            }
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (url.length < 10 || email.isEmpty() || password.isEmpty()) {
                tvError.text = "Tous les champs sont requis"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            btnLogin.isEnabled = false
            progress.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        doLogin(url, email, password)
                    }
                    if (result.first) {
                        val sessionCookie = "PHPSESSID=${result.second}"
                        prefs.edit()
                            .putString("base_url", url)
                            .putString("session_cookie", sessionCookie)
                            .putString("last_email", email)
                            .apply()
                        ApiClient.init(url, sessionCookie)
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        tvError.text = result.third
                        tvError.visibility = View.VISIBLE
                        btnLogin.isEnabled = true
                        progress.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    tvError.text = "Erreur : ${e.message}"
                    tvError.visibility = View.VISIBLE
                    btnLogin.isEnabled = true
                    progress.visibility = View.GONE
                }
            }
        }
    }

    private fun doLogin(baseUrl: String, email: String, password: String): Triple<Boolean, String, String> {
        return try {
            val loginUrl = "${baseUrl}api_login.php"
            val body = FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .build()
            val request = Request.Builder()
                .url(loginUrl)
                .post(body)
                .build()
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val response = client.newCall(request).execute()
            val bodyStr  = response.body?.string() ?: "{}"
            
            // Vérifier que c'est bien du JSON
            if (!bodyStr.trimStart().startsWith("{")) {
                return Triple(false, "", "Reponse serveur invalide (pas JSON)")
            }
            
            val json = JSONObject(bodyStr)
            if (json.optBoolean("ok", false)) {
                Triple(true, json.optString("session_id", ""), json.optString("prenom", ""))
            } else {
                Triple(false, "", json.optString("err", "Erreur inconnue"))
            }
        } catch (e: Exception) {
            Triple(false, "", "Erreur connexion : ${e.message}")
        }
    }
}
