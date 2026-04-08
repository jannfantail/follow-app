package ch.gypaete.follow.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ch.gypaete.follow.R
import ch.gypaete.follow.model.Vol
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class VolListFragment : Fragment() {

    companion object {
        private const val ARG_MODE = "mode"
        fun newInstance(mode: FollowMode) = VolListFragment().apply {
            arguments = Bundle().apply { putSerializable(ARG_MODE, mode) }
        }
    }

    private val vm: FollowViewModel by activityViewModels()
    private lateinit var adapter: VolAdapter
    private lateinit var rvVols: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    private val mode: FollowMode
        get() {
            return if (android.os.Build.VERSION.SDK_INT >= 33) {
                arguments?.getSerializable(ARG_MODE, FollowMode::class.java)
            } else {
                @Suppress("DEPRECATION")
                arguments?.getSerializable(ARG_MODE) as? FollowMode
            } ?: FollowMode.DECO
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_vol_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvVols      = view.findViewById(R.id.rvVols)
        tvEmpty     = view.findViewById(R.id.tvEmpty)
        tvStatus    = view.findViewById(R.id.tvStatus)
        progressBar = view.findViewById(R.id.progressBar)

        adapter = VolAdapter(mode) { vol, action -> handleAction(vol, action) }
        rvVols.layoutManager = LinearLayoutManager(requireContext())
        rvVols.adapter = adapter

        // Spinner rooms
        val spinnerRoom = view.findViewById<android.widget.Spinner>(R.id.spinnerRoom)
        if (spinnerRoom != null) {
            lifecycleScope.launch {
                vm.rooms.collect { rooms ->
                    if (rooms.size > 1) {
                        spinnerRoom.visibility = View.VISIBLE
                        val labels = rooms.map { it.libelle }.toTypedArray()
                        val adp = android.widget.ArrayAdapter(requireContext(),
                            android.R.layout.simple_spinner_item, labels)
                        adp.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item)
                        spinnerRoom.adapter = adp
                        val idx = rooms.indexOfFirst {
                            it.roomCode == vm.uiState.value.room }
                        if (idx >= 0) spinnerRoom.setSelection(idx)
                        spinnerRoom.onItemSelectedListener = object :
                            android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(p: android.widget.AdapterView<*>,
                                v: android.view.View?, pos: Int, id: Long) {
                                val rc = rooms[pos].roomCode
                                if (rc != vm.uiState.value.room) vm.setRoom(rc)
                            }
                            override fun onNothingSelected(
                                p: android.widget.AdapterView<*>) {}
                        }
                    } else {
                        spinnerRoom.visibility = View.GONE
                    }
                }
            }
        }

        view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
            ?.setOnRefreshListener {
                vm.refresh()
                view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
                    ?.isRefreshing = false
            }

        lifecycleScope.launch {
            vm.uiState.collect { state ->
                progressBar.isVisible = state.status == UiStatus.LOADING

                val filtered = filterVols(state.vols, mode)
                adapter.submitList(filtered)
                tvEmpty.isVisible = filtered.isEmpty() && state.status != UiStatus.LOADING

                val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val refreshStr = if (state.lastRefresh > 0) timeFmt.format(state.lastRefresh) else "--:--"
                val liveIcon = if (state.isLive) "LIVE" else "HORS LIGNE"
                val countInAir = state.vols.count { it.statut == "decolle" }
                tvStatus.text = "$liveIcon  |  En l'air : $countInAir  |  $refreshStr"

                tvEmpty.text = when {
                    state.status == UiStatus.ERROR -> "Erreur : ${state.errorMsg}\n\nTirer pour reessayer"
                    mode == FollowMode.DECO        -> "Aucun eleve en attente au deco"
                    else                           -> "Personne en l'air"
                }
            }
        }
    }

    private fun filterVols(vols: List<Vol>, mode: FollowMode): List<Vol> = when (mode) {
        FollowMode.DECO    -> vols.filter { it.status in listOf("wait", "air", "down", "xfer", "cancel") }
        FollowMode.ATTERRO -> vols.filter { it.status in listOf("wait", "air", "down", "xfer") }
    }

    private fun handleAction(vol: Vol, action: String) {
        when (action) {
            "decolle"     -> showExercicesDialog(vol)
            "atterri"     -> confirmAction("Poser ${vol.nomComplet} ?")    { vm.atterri(vol) }
            "annule"      -> confirmAction("Annuler vol de ${vol.nomComplet} ?") { vm.annule(vol) }
            "transfere"   -> confirmAction("Transferer ${vol.nomComplet} ?")  { vm.transfere(vol) }
            "arrive_deco"        -> vm.arriveDeco(vol)
            "attero_valide_deco" -> vm.atteroValideDeco(vol)
        }
    }

    private fun confirmAction(msg: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(msg)
            .setPositiveButton("Confirmer") { _, _ -> onConfirm() }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showExercicesDialog(vol: Vol) {
        val exos = vm.exercices.value.filter { it.libelle.isNotBlank() }
        if (exos.isEmpty()) { vm.decolle(vol, emptyList()); return }

        val checked = BooleanArray(exos.size) { false }
        val ctx = requireContext()

        // ── Layout principal ─────────────────────────────────────────────
        val root = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        // ── En-tete ──────────────────────────────────────────────────────
        val header = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#1A237E"))
            setPadding(48, 40, 48, 32)
        }
        header.addView(android.widget.TextView(ctx).apply {
            text = "Exercices FSVL"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#90CAF9"))
            letterSpacing = 0.12f
        })
        header.addView(android.widget.TextView(ctx).apply {
            text = vol.nomComplet
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 4, 0, 0)
        })
        root.addView(header)

        // ── Liste scrollable ─────────────────────────────────────────────
        val scroll = android.widget.ScrollView(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0).apply { weight = 1f }
        }
        val list = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val checkBoxes = mutableListOf<android.widget.CheckBox>()
        exos.forEachIndexed { i, exo ->
            val row = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(16, 0, 16, 0)
                setBackgroundColor(
                    if (i % 2 == 0) android.graphics.Color.WHITE
                    else android.graphics.Color.parseColor("#F8F9FF")
                )
            }
            // Numero
            val tvNum = android.widget.TextView(ctx).apply {
                text = "${i + 1}"
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#1565C0"))
                width = 60
                gravity = android.view.Gravity.CENTER
            }
            // Checkbox + label
            val cb = android.widget.CheckBox(ctx).apply {
                text = exo.libelle
                textSize = 15f
                setTextColor(android.graphics.Color.parseColor("#1A1A2E"))
                setPadding(8, 20, 16, 20)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnCheckedChangeListener { _, isChecked ->
                    checked[i] = isChecked
                    row.setBackgroundColor(
                        if (isChecked) android.graphics.Color.parseColor("#E3F2FD")
                        else if (i % 2 == 0) android.graphics.Color.WHITE
                        else android.graphics.Color.parseColor("#F8F9FF")
                    )
                }
            }
            checkBoxes.add(cb)
            row.addView(tvNum)
            row.addView(cb)
            list.addView(row)
        }
        scroll.addView(list)
        root.addView(scroll)

        // ── Compteur selection ───────────────────────────────────────────
        val tvCount = android.widget.TextView(ctx).apply {
            text = "Aucun exercice selectionne"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#607D8B"))
            gravity = android.view.Gravity.CENTER
            setPadding(16, 12, 16, 8)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        }
        // Mettre a jour le compteur quand on coche
        checkBoxes.forEachIndexed { i, cb ->
            cb.setOnCheckedChangeListener { _, isChecked ->
                checked[i] = isChecked
                val n = checked.count { it }
                tvCount.text = if (n == 0) "Aucun exercice selectionne"
                               else "$n exercice(s) selectionne(s)"
                val row = list.getChildAt(i) as android.widget.LinearLayout
                row.setBackgroundColor(
                    if (isChecked) android.graphics.Color.parseColor("#E3F2FD")
                    else if (i % 2 == 0) android.graphics.Color.WHITE
                    else android.graphics.Color.parseColor("#F8F9FF")
                )
            }
        }
        root.addView(tvCount)

        // ── Boutons ──────────────────────────────────────────────────────
        val btnRow = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            weightSum = 3f
            setPadding(12, 12, 12, 16)
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setView(root).create()

        btnRow.addView(android.widget.Button(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, 108, 1f).apply { marginEnd = 6 }
            text = "Annuler"
            textSize = 13f
            setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
            setTextColor(android.graphics.Color.parseColor("#424242"))
            setOnClickListener { dialog.dismiss() }
        })
        btnRow.addView(android.widget.Button(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, 108, 1f).apply { marginEnd = 6 }
            text = "Sans exo"
            textSize = 13f
            setBackgroundColor(android.graphics.Color.parseColor("#607D8B"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { dialog.dismiss(); vm.decolle(vol, emptyList()) }
        })
        btnRow.addView(android.widget.Button(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0, 108, 1f)
            text = "Decollage"
            textSize = 13f
            setBackgroundColor(android.graphics.Color.parseColor("#1565C0"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                dialog.dismiss()
                val ids = exos.filterIndexed { i, _ -> checked[i] }.map { it.id }
                vm.decolle(vol, ids)
            }
        })
        root.addView(btnRow)

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.93).toInt(),
            (resources.displayMetrics.heightPixels * 0.82).toInt()
        )
    }
}
