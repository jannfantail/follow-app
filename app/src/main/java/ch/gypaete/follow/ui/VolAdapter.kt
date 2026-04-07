package ch.gypaete.follow.ui

import android.graphics.Color
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ch.gypaete.follow.R
import ch.gypaete.follow.model.Vol

class VolAdapter(
    private val mode: FollowMode,
    private val onAction: (Vol, String) -> Unit
) : ListAdapter<Vol, VolAdapter.VolViewHolder>(VolDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VolViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vol, parent, false)
        return VolViewHolder(view)
    }

    override fun onBindViewHolder(holder: VolViewHolder, position: Int) {
        holder.bind(getItem(position), mode, onAction)
    }

    class VolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvInitiales  = itemView.findViewById<TextView>(R.id.tvInitiales)
        private val tvNom        = itemView.findViewById<TextView>(R.id.tvNom)
        private val tvStatut     = itemView.findViewById<TextView>(R.id.tvStatut)
        private val tvExos       = itemView.findViewById<TextView>(R.id.tvExos)
        private val tvTime       = itemView.findViewById<TextView>(R.id.tvTime)
        private val btnPrimary   = itemView.findViewById<Button>(R.id.btnPrimary)
        private val btnSecondary = itemView.findViewById<Button>(R.id.btnSecondary)
        private val cardView     = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.cardView)

        fun bind(vol: Vol, mode: FollowMode, onAction: (Vol, String) -> Unit) {
            tvInitiales.text = vol.initiales
            tvNom.text = vol.nomComplet

            val statutUI = when (vol.lastEventType?.uppercase()) {
                "TRANSFERE"          -> "transfere"
                "ARRIVEE_DECO"       -> "arrive_deco"
                "ATTERO_VALIDE_DECO" -> "arrive_deco"
                else                  -> vol.statut
            }

            val (statutLabel, statutColor) = when (statutUI) {
                "en_attente"  -> "En attente" to "#607D8B"
                "decolle"     -> "En l'air"   to "#1565C0"
                "atterri"     -> "Pose"        to "#2E7D32"
                "annule"      -> "Annule"      to "#B71C1C"
                "transfere"   -> "Transfere"   to "#6A1B9A"
                "arrive_deco" -> "Au deco"     to "#E65100"
                else            -> statutUI       to "#757575"
            }
            tvStatut.text = statutLabel
            tvStatut.setBackgroundColor(Color.parseColor(statutColor))

            if (vol.exercices.isNotEmpty()) {
                tvExos.visibility = View.VISIBLE
                tvExos.text = vol.exercices.joinToString("  ") {
                    "[${it.categorie} ${it.numero}] ${it.libelle}"
                }
            } else {
                tvExos.visibility = View.GONE
            }

            if (vol.nbVols > 0) {
                tvTime.visibility = View.VISIBLE
                tvTime.text = "x${vol.nbVols}"
            } else {
                tvTime.visibility = View.GONE
            }

            cardView.setCardBackgroundColor(when (statutUI) {
                "decolle"     -> Color.parseColor("#E3F2FD")
                "atterri"     -> Color.parseColor("#E8F5E9")
                "annule"      -> Color.parseColor("#FFEBEE")
                "transfere"   -> Color.parseColor("#F3E5F5")
                "arrive_deco" -> Color.parseColor("#FFF3E0")
                else           -> Color.WHITE
            })

            when (mode) {
                FollowMode.DECO -> {
                    val show = vol.statut == "en_attente"
                    btnPrimary.visibility   = if (show) View.VISIBLE else View.GONE
                    btnSecondary.visibility = if (show) View.VISIBLE else View.GONE
                    btnPrimary.text = "Decollage"
                    btnPrimary.setBackgroundColor(Color.parseColor("#1565C0"))
                    btnSecondary.text = "Annuler"
                    btnSecondary.setBackgroundColor(Color.parseColor("#B71C1C"))
                    btnPrimary.setOnClickListener   { onAction(vol, "decolle") }
                    btnSecondary.setOnClickListener { onAction(vol, "annule") }
                }
                FollowMode.ATTERRO -> {
                    val enAir = vol.statut == "decolle"
                    btnPrimary.visibility   = if (enAir) View.VISIBLE else View.GONE
                    btnSecondary.visibility = if (enAir) View.VISIBLE else View.GONE
                    btnPrimary.text = "Pose"
                    btnPrimary.setBackgroundColor(Color.parseColor("#2E7D32"))
                    btnSecondary.text = "Transfere"
                    btnSecondary.setBackgroundColor(Color.parseColor("#6A1B9A"))
                    btnPrimary.setOnClickListener   { onAction(vol, "atterri") }
                    btnSecondary.setOnClickListener { onAction(vol, "transfere") }
                }
            }
        }
    }

    class VolDiffCallback : DiffUtil.ItemCallback<Vol>() {
        override fun areItemsTheSame(o: Vol, n: Vol) = o.volId == n.volId
        override fun areContentsTheSame(o: Vol, n: Vol) = o == n
    }
}
