package ch.gypaete.follow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ch.gypaete.follow.R
import ch.gypaete.follow.api.ApiClient
import ch.gypaete.follow.util.SoundManager
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val vm: FollowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val prefs  = getSharedPreferences("follow", Context.MODE_PRIVATE)
            val url    = prefs.getString("base_url", "") ?: ""
            val cookie = prefs.getString("session_cookie", "") ?: ""

            if (url.isEmpty() || cookie.isEmpty()) {
                goToLogin()
                return
            }

            ApiClient.init(url, cookie)

        } catch (e: Exception) {
            goToLogin()
            return
        }

        try {
            setContentView(R.layout.activity_main)
            setSupportActionBar(findViewById(R.id.toolbar))

            val tabs = findViewById<TabLayout>(R.id.tabs)
            tabs.addTab(tabs.newTab().setText("Deco"))
            tabs.addTab(tabs.newTab().setText("Atterro"))

            tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    val mode = if (tab.position == 0) FollowMode.DECO else FollowMode.ATTERRO
                    vm.setMode(mode)
                    showFragment(mode)
                }
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })

            showFragment(FollowMode.DECO)

            lifecycleScope.launch {
                vm.toast.collect { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }

            lifecycleScope.launch {
                vm.soundAction.collect { action ->
                    try {
                        SoundManager.playForAction(this@MainActivity, action)
                    } catch (e: Exception) {
                        // silencieux si son échoue
                    }
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
            goToLogin()
        }
    }

    private fun showFragment(mode: FollowMode) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, VolListFragment.newInstance(mode))
                .commit()
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh  -> { vm.refresh(); true }
            R.id.action_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
            R.id.action_logout   -> { goToLogin(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun goToLogin() {
        try {
            getSharedPreferences("follow", Context.MODE_PRIVATE)
                .edit().remove("session_cookie").apply()
            startActivity(Intent(this, LoginActivity::class.java))
        } catch (e: Exception) {
            // ignore
        }
        finish()
    }
}
