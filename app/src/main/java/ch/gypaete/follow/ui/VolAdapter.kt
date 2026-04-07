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
            tvNom.text       = vol.nomComplet

            // Badge statut UI étendu
            val (label, color) = when (vol.statutUI) {
                "en_attente"  -> "En attente"  to "#607D8B"
                "decolle"     -> "En l'air"     to "#1565C0"
                "atterri"     -> "Pose"          to "#2E7D32"
                "annule"      -> "Annule"        to "#B71C1C"
                "transfere"   -> "Transfere"     to "#6A1B9A"
                "arrive_deco" -> "Au deco"       to "#E65100"
                else          -> vol.statutUI    to "#757575"
            }
            tvStatut.text = label
            tvStatut.setBackgroundColor(Color.parseColor(color))

            // Exercices
            if (vol.exercices.isNotEmpty()) {
                tvExos.visibility = View.VISIBLE
                tvExos.text = vol.exercices.joinToString("  ") {
                    "[${it.categorie} ${it.numero}] ${it.libelle}"
                }
            } else {
                tvExos.visibility = View.GONE
            }

            // Nb vols
            if (vol.nbVols > 0) {
                tvTime.visibility = View.VISIBLE
                tvTime.text = "x${vol.nbVols}"
            } else {
                tvTime.visibility = View.GONE
            }

            // Couleur carte
            cardView.setCardBackgroundColor(when (vol.statutUI) {
                "decolle"     -> Color.parseColor("#E3F2FD")
                "atterri"     -> Color.parseColor("#E8F5E9")
                "annule"      -> Color.parseColor("#FFEBEE")
                "transfere"   -> Color.parseColor("#F3E5F5")
                "arrive_deco" -> Color.parseColor("#FFF3E0")
                else          -> Color.WHITE
            })

            // Boutons selon mode
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

                    // Bouton primaire change selon last_event_type
                    when (vol.statutUI) {
                        "arrive_deco" -> {
                            // Arrivé au déco mais pas encore décollé officiellement
                            btnPrimary.text = "Pose"
                            btnPrimary.setBackgroundColor(Color.parseColor("#2E7D32"))
                            btnSecondary.text = "Transfere"
                            btnSecondary.setBackgroundColor(Color.parseColor("#6A1B9A"))
                        }
                        else -> {
                            btnPrimary.text = "Pose"
                            btnPrimary.setBackgroundColor(Color.parseColor("#2E7D32"))
                            btnSecondary.text = "Transfere"
                            btnSecondary.setBackgroundColor(Color.parseColor("#6A1B9A"))
                        }
                    }
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
