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
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val vm: FollowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Vérifier config
        val prefs = getSharedPreferences("follow", Context.MODE_PRIVATE)
        val url    = prefs.getString("base_url", "") ?: ""
        val cookie = prefs.getString("session_cookie", "") ?: ""

        if (url.isEmpty()) {
            goToLogin()
            return
        }

        ApiClient.init(url, cookie)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        // Tabs Déco / Atterro
        val tabs = findViewById<TabLayout>(R.id.tabs)
        tabs.addTab(tabs.newTab().setText("⛰  Déco"))
        tabs.addTab(tabs.newTab().setText("🟢 Atterro"))

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val mode = if (tab.position == 0) FollowMode.DECO else FollowMode.ATTERRO
                vm.setMode(mode)
                showFragment(mode)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Charger fragment initial
        showFragment(FollowMode.DECO)

        // Toast one-shot
        lifecycleScope.launch {
            vm.toast.collect { msg ->
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFragment(mode: FollowMode) {
        val fragment = VolListFragment.newInstance(mode)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh  -> { vm.refresh(); true }
            R.id.action_settings -> { goToLogin(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
