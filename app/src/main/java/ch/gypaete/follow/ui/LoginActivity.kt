package ch.gypaete.follow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import ch.gypaete.follow.R
import ch.gypaete.follow.api.ApiClient

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("follow", Context.MODE_PRIVATE)

        val etUrl    = findViewById<EditText>(R.id.etUrl)
        val etCookie = findViewById<EditText>(R.id.etCookie)
        val btnSave  = findViewById<Button>(R.id.btnSave)

        // Pré-remplir
        etUrl.setText(prefs.getString("base_url", "https://mon-ecole.ch/content/follow/"))
        etCookie.setText(prefs.getString("session_cookie", ""))

        btnSave.setOnClickListener {
            val url    = etUrl.text.toString().trim()
            val cookie = etCookie.text.toString().trim()

            if (url.isEmpty()) {
                etUrl.error = "URL obligatoire"
                return@setOnClickListener
            }

            prefs.edit()
                .putString("base_url", url)
                .putString("session_cookie", cookie)
                .apply()

            ApiClient.init(url, cookie)

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
