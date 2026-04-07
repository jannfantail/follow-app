package ch.gypaete.follow.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import ch.gypaete.follow.R
import ch.gypaete.follow.util.SoundManager

class SettingsActivity : AppCompatActivity() {

    private val actions = listOf(
        "decolle"            to "Decollage",
        "atterri"            to "Atterrissage (pose)",
        "annule"             to "Annulation",
        "transfere"          to "Transfere",
        "arrive_deco"        to "Arrive au deco",
        "attero_valide_deco" to "Vu deco (atterro)"
    )

    // Code de retour par action pour le picker de sonnerie
    private val actionPickerCodes = actions.mapIndexed { i, (key, _) -> (200 + i) to key }.toMap()
    private var currentPickerAction = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Parametres"

        val container = findViewById<LinearLayout>(R.id.settingsContainer)
        val prefs = getSharedPreferences("follow", Context.MODE_PRIVATE)

        // ── Toggle notifications ──────────────────────────────────────────
        val tvNotif = TextView(this).apply {
            text = "Notifications (ecran eteint)"
            textSize = 15f
            setTextColor(0xFF1A1A2E.toInt())
            setPadding(0, 24, 0, 6)
        }
        container.addView(tvNotif)

        @Suppress("DEPRECATION")
        val swNotif = Switch(this).apply {
            isChecked = prefs.getBoolean("notif_enabled", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("notif_enabled", checked).apply()
            }
        }
        container.addView(swNotif)

        // ── Separateur ───────────────────────────────────────────────────
        val tvTitle = TextView(this).apply {
            text = "Sons par action"
            textSize = 16f
            setTextColor(0xFF1A1A2E.toInt())
            setPadding(0, 32, 0, 8)
        }
        container.addView(tvTitle)

        val soundKeys   = SoundManager.SOUNDS.keys.toList()
        val soundLabels = SoundManager.SOUNDS.values.toList()

        actions.forEachIndexed { index, (actionKey, actionLabel) ->
            // Label action
            val tv = TextView(this).apply {
                text = actionLabel
                textSize = 14f
                setTextColor(0xFF1A1A2E.toInt())
                setPadding(0, 20, 0, 4)
            }
            container.addView(tv)

            // Ligne : Spinner + bouton Choisir
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 1f
            }

            // Spinner
            val spinner = Spinner(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f)
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, soundLabels)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            val current = SoundManager.getSoundForAction(this, actionKey)
            val idx = soundKeys.indexOf(current).takeIf { it >= 0 } ?: 0
            spinner.setSelection(idx)

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>, v: android.view.View?, pos: Int, id: Long) {
                    val key = soundKeys[pos]
                    SoundManager.saveSoundForAction(this@SettingsActivity, actionKey, key)
                    if (key != "aucun") SoundManager.play(this@SettingsActivity, key)
                }
                override fun onNothingSelected(p: AdapterView<*>) {}
            }
            row.addView(spinner)

            // Bouton choisir sonnerie
            val btnPick = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f)
                text = "Choisir"
                textSize = 11f
                setOnClickListener {
                    currentPickerAction = actionKey
                    val customUri = SoundManager.getCustomUriForAction(
                        this@SettingsActivity, actionKey)
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                            RingtoneManager.TYPE_ALL)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                            "Son pour $actionLabel")
                        if (customUri != null) {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                Uri.parse(customUri))
                        }
                    }
                    startActivityForResult(intent, 200 + index)
                }
            }
            row.addView(btnPick)
            container.addView(row)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val actionKey = actionPickerCodes[requestCode] ?: return
            val uri = data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                SoundManager.saveSoundForAction(this, actionKey, "custom", uri.toString())
                SoundManager.play(this, "custom", uri.toString())
                Toast.makeText(this, "Sonnerie enregistree", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
