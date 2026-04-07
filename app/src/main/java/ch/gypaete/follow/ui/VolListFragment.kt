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
        FollowMode.DECO    -> vols.filter { it.status in listOf("wait", "xfer", "cancel") }
        FollowMode.ATTERRO -> vols.filter { it.status in listOf("air", "down", "xfer") }
    }

    private fun handleAction(vol: Vol, action: String) {
        when (action) {
            "decolle"     -> showExercicesDialog(vol)
            "atterri"     -> confirmAction("Poser ${vol.nomComplet} ?")    { vm.atterri(vol) }
            "annule"      -> confirmAction("Annuler vol de ${vol.nomComplet} ?") { vm.annule(vol) }
            "transfere"   -> confirmAction("Transferer ${vol.nomComplet} ?")  { vm.transfere(vol) }
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

    private fun showExercicesDialog(vol: Vol) {
        val exos = vm.exercices.value
        if (exos.isEmpty()) { vm.decolle(vol, emptyList()); return }

        val labels  = exos.map { "[${it.categorie} ${it.numero}] ${it.libelle}" }.toTypedArray()
        val checked = BooleanArray(exos.size) { false }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Exercices FSVL — ${vol.nomComplet}")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setPositiveButton("Decollage") { _, _ ->
                val ids = exos.filterIndexed { i, _ -> checked[i] }.map { it.id }
                vm.decolle(vol, ids)
            }
            .setNeutralButton("Sans exercice") { _, _ -> vm.decolle(vol, emptyList()) }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
