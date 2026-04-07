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

            // Statut badge
            val (statutLabel, statutColor) = when (vol.statut) {
                "en_attente" -> "En attente" to "#607D8B"
                "decolle"    -> "En l'air"   to "#1565C0"
                "atterri"    -> "Pose"        to "#2E7D32"
                "annule"     -> "Annule"      to "#B71C1C"
                else         -> vol.statut    to "#757575"
            }
            tvStatut.text = statutLabel
            tvStatut.setBackgroundColor(Color.parseColor(statutColor))

            // Exercices
            if (vol.exercices.isNotEmpty()) {
                tvExos.visibility = View.VISIBLE
                tvExos.text = vol.exercices.joinToString(" - ") {
                    "${it.categorie} ${it.numero}"
                }
            } else {
                tvExos.visibility = View.GONE
            }

            // Heure (nb vols)
            if (vol.nbVols > 0) {
                tvTime.visibility = View.VISIBLE
                tvTime.text = "x${vol.nbVols}"
            } else {
                tvTime.visibility = View.GONE
            }

            // Couleur carte
            val cardColor = when (vol.statut) {
                "decolle" -> Color.parseColor("#E3F2FD")
                "atterri" -> Color.parseColor("#E8F5E9")
                "annule"  -> Color.parseColor("#FFEBEE")
                else      -> Color.WHITE
            }
            cardView.setCardBackgroundColor(cardColor)

            // Boutons selon mode
            when (mode) {
                FollowMode.DECO -> {
                    btnPrimary.text = "Decollage"
                    btnPrimary.setBackgroundColor(Color.parseColor("#1565C0"))
                    btnPrimary.visibility = if (vol.statut == "en_attente") View.VISIBLE else View.GONE
                    btnPrimary.setOnClickListener { onAction(vol, "decolle") }

                    btnSecondary.text = "Annuler"
                    btnSecondary.visibility = if (vol.statut == "en_attente") View.VISIBLE else View.GONE
                    btnSecondary.setOnClickListener { onAction(vol, "annule") }
                }
                FollowMode.ATTERRO -> {
                    btnPrimary.text = "Pose"
                    btnPrimary.setBackgroundColor(Color.parseColor("#2E7D32"))
                    btnPrimary.visibility = if (vol.statut == "decolle") View.VISIBLE else View.GONE
                    btnPrimary.setOnClickListener { onAction(vol, "atterri") }

                    btnSecondary.text = "Transfere"
                    btnSecondary.visibility = if (vol.statut == "decolle") View.VISIBLE else View.GONE
                    btnSecondary.setOnClickListener { onAction(vol, "transfere") }
                }
            }
        }
    }

    class VolDiffCallback : DiffUtil.ItemCallback<Vol>() {
        override fun areItemsTheSame(oldItem: Vol, newItem: Vol) = oldItem.volId == newItem.volId
        override fun areContentsTheSame(oldItem: Vol, newItem: Vol) = oldItem == newItem
    }
}
