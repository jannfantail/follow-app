package ch.gypaete.follow.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import ch.gypaete.follow.R
import ch.gypaete.follow.util.SoundManager

class SettingsActivity : AppCompatActivity() {

    // Actions pour lesquelles on peut configurer un son
    private val actions = listOf(
        "decolle"            to "Decollage",
        "atterri"            to "Atterrissage (pose)",
        "annule"             to "Annulation",
        "transfere"          to "Transfere",
        "arrive_deco"        to "Arrive au deco",
        "attero_valide_deco" to "Vu deco (atterro)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Parametres"

        val container = findViewById<LinearLayout>(R.id.settingsContainer)

        // ── Toggle notifications ──────────────────────────────────────────
        val prefs = getSharedPreferences("follow", Context.MODE_PRIVATE)
        val tvNotif = TextView(this).apply {
            text = "Notifications (ecran eteint)"
            textSize = 15f
            setTextColor(0xFF1A1A2E.toInt())
            setPadding(0, 24, 0, 6)
        }
        container.addView(tvNotif)
        val swNotif = android.widget.Switch(this).apply {
            isChecked = prefs.getBoolean("notif_enabled", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("notif_enabled", checked).apply()
            }
        }
        container.addView(swNotif)

        val divider = TextView(this).apply {
            text = "Sons par action"
            textSize = 16f
            setTextColor(0xFF1A1A2E.toInt())
            setPadding(0, 32, 0, 8)
        }
        container.addView(divider)

        val soundKeys   = SoundManager.SOUNDS.keys.toList()
        val soundLabels = SoundManager.SOUNDS.values.toList()

        actions.forEach { (actionKey, actionLabel) ->
            // Label
            val tv = TextView(this).apply {
                text = actionLabel
                textSize = 15f
                setTextColor(0xFF1A1A2E.toInt())
                setPadding(0, 24, 0, 6)
            }
            container.addView(tv)

            // Spinner
            val spinner = Spinner(this)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, soundLabels)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            // Sélectionner la valeur sauvegardée
            val current = SoundManager.getSoundForAction(this, actionKey)
            val idx = soundKeys.indexOf(current).takeIf { it >= 0 } ?: 0
            spinner.setSelection(idx)

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, pos: Int, id: Long) {
                    val key = soundKeys[pos]
                    SoundManager.saveSoundForAction(this@SettingsActivity, actionKey, key)
                    // Prévisualiser le son
                    SoundManager.play(this@SettingsActivity, key)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
            container.addView(spinner)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
