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
import ch.gypaete.follow.model.Exercice
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
        get() = arguments?.getSerializable(ARG_MODE) as? FollowMode ?: FollowMode.DECO

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_vol_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvVols      = view.findViewById(R.id.rvVols)
        tvEmpty     = view.findViewById(R.id.tvEmpty)
        tvStatus    = view.findViewById(R.id.tvStatus)
        progressBar = view.findViewById(R.id.progressBar)

        adapter = VolAdapter(mode) { vol, action ->
            handleAction(vol, action)
        }

        rvVols.layoutManager = LinearLayoutManager(requireContext())
        rvVols.adapter = adapter

        // Swipe-to-refresh (pull down)
        view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
            ?.setOnRefreshListener {
                vm.refresh()
                view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
                    ?.isRefreshing = false
            }

        // Observer
        lifecycleScope.launch {
            vm.uiState.collect { state ->
                progressBar.isVisible = state.status == UiStatus.LOADING

                val filtered = filterVols(state.vols, mode)
                adapter.submitList(filtered)
                tvEmpty.isVisible = filtered.isEmpty() && state.status != UiStatus.LOADING

                // Barre de statut live
                val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val refreshStr = if (state.lastRefresh > 0) timeFmt.format(state.lastRefresh) else "--:--"
                val liveIcon = if (state.isLive) "🟢" else "🔴"
                val countInAir = state.vols.count { it.statut == "decolle" }
                tvStatus.text = "$liveIcon Live  •  En l'air : $countInAir  •  ${refreshStr}"

                if (state.status == UiStatus.ERROR) {
                    tvEmpty.text = "❌ ${state.errorMsg}\n\nTire vers le bas pour réessayer"
                    tvEmpty.isVisible = true
                } else {
                    tvEmpty.text = when (mode) {
                        FollowMode.DECO    -> "Aucun élève en attente au déco"
                        FollowMode.ATTERRO -> "Personne en l'air pour l'instant"
                    }
                }
            }
        }
    }

    private fun filterVols(vols: List<Vol>, mode: FollowMode): List<Vol> = when (mode) {
        FollowMode.DECO    -> vols.filter { it.statut in listOf("en_attente", "annule") }
        FollowMode.ATTERRO -> vols.filter { it.statut == "decolle" }
    }

    // ── Gestion des actions ────────────────────────────────────────────────
    private fun handleAction(vol: Vol, action: String) {
        when (action) {
            "decolle"     -> showExercicesDialog(vol)
            "atterri"     -> confirmAction("Poser ${vol.prenom} ?") { vm.atterri(vol) }
            "annule"      -> confirmAction("Annuler le vol de ${vol.prenom} ?") { vm.annule(vol) }
            "transfere"   -> confirmAction("Transférer ${vol.prenom} ?") { vm.transfere(vol) }
            "arrive_deco" -> vm.arriveDeco(vol)
        }
    }

    private fun confirmAction(msg: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(msg)
            .setPositiveButton("Confirmer") { _, _ -> onConfirm() }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // ── Dialog sélection exercices FSVL ───────────────────────────────────
    private fun showExercicesDialog(vol: Vol) {
        val exos = vm.exercices.value
        if (exos.isEmpty()) {
            // Pas d'exercices → décolle direct
            vm.decolle(vol, emptyList())
            return
        }

        // Grouper par catégorie
        val grouped = exos.groupBy { it.categorie }
        val labels  = exos.map { "[${it.categorie} ${it.numero}] ${it.libelle}" }.toTypedArray()
        val checked = BooleanArray(exos.size) { false }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("✈ ${vol.prenom} — exercices FSVL")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("Décollage") { _, _ ->
                val selectedIds = exos
                    .filterIndexed { i, _ -> checked[i] }
                    .map { it.id }
                vm.decolle(vol, selectedIds)
            }
            .setNeutralButton("Sans exercice") { _, _ ->
                vm.decolle(vol, emptyList())
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
