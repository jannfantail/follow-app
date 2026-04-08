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
            "decolle"            -> showExercicesDialog(vol)
            "atterri"            -> confirmAction("Poser ${vol.nomComplet} ?") { vm.atterri(vol) }
            "annule"             -> confirmAction("Annuler vol de ${vol.nomComplet} ?") { vm.annule(vol) }
            "transfere"          -> confirmAction("Transferer ${vol.nomComplet} ?") { vm.transfere(vol) }
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
        val exos = vm.exercices.value
        if (exos.isEmpty()) { vm.decolle(vol, emptyList()); return }
        // Si tous les libelles sont vides -> decollage direct
        val exosValides = exos.filter { it.libelle.isNotBlank() }
        if (exosValides.isEmpty()) { vm.decolle(vol, emptyList()); return }

        val checked = BooleanArray(exosValides.size) { false }

        // Layout personnalise
        val ctx = requireContext()
        val layout = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(0, 8, 0, 0)
        }

        // Titre
        val tvTitre = android.widget.TextView(ctx).apply {
            text = vol.nomComplet
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#1A237E"))
            setPadding(48, 16, 48, 4)
        }
        layout.addView(tvTitre)

        val tvSub = android.widget.TextView(ctx).apply {
            text = "Selectionnez les exercices pour ce vol"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#607D8B"))
            setPadding(48, 0, 48, 16)
        }
        layout.addView(tvSub)

        // Separateur
        val sep = android.view.View(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
        }
        layout.addView(sep)

        // ScrollView avec liste
        val scroll = android.widget.ScrollView(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0).apply {
                weight = 1f
            }
        }
        val listLayout = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }

        exosValides.forEachIndexed { i, exo ->
            val label = exo.libelle.ifBlank { "Exercice ${i+1}" }
            val cb = android.widget.CheckBox(ctx).apply {
                text = label
                textSize = 15f
                setTextColor(android.graphics.Color.parseColor("#212121"))
                setPadding(48, 4, 48, 4)
                setOnCheckedChangeListener { _, isChecked -> checked[i] = isChecked }
            }
            listLayout.addView(cb)

            // Ligne separateur leger
            if (i < exosValides.size - 1) {
                listLayout.addView(android.view.View(ctx).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                        marginStart = 48; marginEnd = 48
                    }
                    setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
                })
            }
        }
        scroll.addView(listLayout)
        layout.addView(scroll)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setView(layout)
            .create()

        // Boutons personnalises sur la meme ligne
        val btnRow = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(16, 12, 16, 12)
            weightSum = 3f
            setBackgroundColor(android.graphics.Color.parseColor("#F8F8F8"))
        }

        val btnAnnuler = android.widget.Button(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 4
            }
            text = "Annuler"
            setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
            setTextColor(android.graphics.Color.parseColor("#424242"))
            setOnClickListener { dialog.dismiss() }
        }

        val btnSans = android.widget.Button(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 4
            }
            text = "Sans exo"
            setBackgroundColor(android.graphics.Color.parseColor("#607D8B"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                dialog.dismiss()
                vm.decolle(vol, emptyList())
            }
        }

        val btnDeco = android.widget.Button(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "Decollage"
            setBackgroundColor(android.graphics.Color.parseColor("#1565C0"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                dialog.dismiss()
                val ids = exosValides.filterIndexed { i, _ -> checked[i] }.map { it.id }
                vm.decolle(vol, ids)
            }
        }

        btnRow.addView(btnAnnuler)
        btnRow.addView(btnSans)
        btnRow.addView(btnDeco)
        layout.addView(btnRow)

        dialog.show()

        // Hauteur max 80% de l'ecran
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            (resources.displayMetrics.heightPixels * 0.80).toInt()
        )
    }
}
