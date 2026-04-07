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
            val vuDeco = vol.lastEventType?.uppercase() == "ATTERO_VALIDE_DECO"
            val (statutLabel, statutColor) = when (vol.status) {
                "wait"   -> "En attente"               to "#607D8B"
                "air"    -> if (vuDeco) "VALIDE" else "En l'air" to if (vuDeco) "#2E7D32" else "#1565C0"
                "down"   -> "Pose"                     to "#2E7D32"
                "cancel" -> "Annule"                   to "#B71C1C"
                "xfer"   -> "Transfere"                to "#6A1B9A"
                else     -> vol.status                 to "#757575"
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
            val cardColor = when (vol.status) {
                "air"    -> if (vuDeco) Color.parseColor("#E8F5E9") else Color.parseColor("#E3F2FD")
                "down"   -> Color.parseColor("#E8F5E9")
                "cancel" -> Color.parseColor("#FFEBEE")
                "xfer"   -> Color.parseColor("#F3E5F5")
                else     -> Color.WHITE
            }
            cardView.setCardBackgroundColor(cardColor)

            // Boutons selon mode
            when (mode) {
                FollowMode.DECO -> {
                    when (vol.status) {
                        "wait" -> {
                            btnPrimary.text = "Decollage"
                            btnPrimary.setBackgroundColor(Color.parseColor("#1565C0"))
                            btnPrimary.setTextColor(Color.WHITE)
                            btnPrimary.visibility = View.VISIBLE
                            btnPrimary.isEnabled = true
                            btnPrimary.setOnClickListener { onAction(vol, "decolle") }
                            btnSecondary.text = "Annuler"
                            btnSecondary.setBackgroundColor(Color.parseColor("#C62828"))
                            btnSecondary.setTextColor(Color.WHITE)
                            btnSecondary.visibility = View.VISIBLE
                            btnSecondary.isEnabled = true
                            btnSecondary.setOnClickListener { onAction(vol, "annule") }
                        }
                        "air" -> {
                            btnPrimary.text = "Decollage"
                            btnPrimary.setBackgroundColor(Color.parseColor("#B0BEC5"))
                            btnPrimary.setTextColor(Color.parseColor("#78909C"))
                            btnPrimary.visibility = View.VISIBLE
                            btnPrimary.isEnabled = false
                            btnPrimary.setOnClickListener(null)
                            btnSecondary.text = "Annuler"
                            btnSecondary.setBackgroundColor(Color.parseColor("#C62828"))
                            btnSecondary.setTextColor(Color.WHITE)
                            btnSecondary.visibility = View.VISIBLE
                            btnSecondary.isEnabled = true
                            btnSecondary.setOnClickListener { onAction(vol, "annule") }
                        }
                        "xfer" -> {
                            btnPrimary.text = "Arrive au deco"
                            btnPrimary.setBackgroundColor(Color.parseColor("#1565C0"))
                            btnPrimary.setTextColor(Color.WHITE)
                            btnPrimary.visibility = View.VISIBLE
                            btnPrimary.isEnabled = true
                            btnPrimary.setOnClickListener { onAction(vol, "arrive_deco") }
                            btnSecondary.text = "Annuler"
                            btnSecondary.setBackgroundColor(Color.parseColor("#C62828"))
                            btnSecondary.setTextColor(Color.WHITE)
                            btnSecondary.visibility = View.VISIBLE
                            btnSecondary.isEnabled = true
                            btnSecondary.setOnClickListener { onAction(vol, "annule") }
                        }
                        else -> {
                            btnPrimary.visibility = View.GONE
                            btnSecondary.visibility = View.GONE
                        }
                    }
                }
                FollowMode.ATTERRO -> {
                    when (vol.status) {
                        "air" -> {
                            btnPrimary.text = "Pose"
                            btnPrimary.setBackgroundColor(Color.parseColor("#2E7D32"))
                            btnPrimary.setTextColor(Color.WHITE)
                            btnPrimary.visibility = View.VISIBLE
                            btnPrimary.isEnabled = true
                            btnPrimary.setOnClickListener { onAction(vol, "atterri") }
                            btnSecondary.text = "Transfere"
                            btnSecondary.setBackgroundColor(Color.parseColor("#9E9E9E"))
                            btnSecondary.setTextColor(Color.WHITE)
                            btnSecondary.visibility = View.VISIBLE
                            btnSecondary.isEnabled = false
                            btnSecondary.setOnClickListener(null)
                        }
                        "down" -> {
                            btnPrimary.text = "Pose"
                            btnPrimary.setBackgroundColor(Color.parseColor("#9E9E9E"))
                            btnPrimary.setTextColor(Color.WHITE)
                            btnPrimary.visibility = View.VISIBLE
                            btnPrimary.isEnabled = false
                            btnPrimary.setOnClickListener(null)
                            btnSecondary.text = "Transfere"
                            btnSecondary.setBackgroundColor(Color.parseColor("#6A1B9A"))
                            btnSecondary.setTextColor(Color.WHITE)
                            btnSecondary.visibility = View.VISIBLE
                            btnSecondary.isEnabled = true
                            btnSecondary.setOnClickListener { onAction(vol, "transfere") }
                        }
                        else -> {
                            btnPrimary.visibility = View.GONE
                            btnSecondary.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    class VolDiffCallback : DiffUtil.ItemCallback<Vol>() {
        override fun areItemsTheSame(oldItem: Vol, newItem: Vol) = oldItem.volId == newItem.volId
        override fun areContentsTheSame(oldItem: Vol, newItem: Vol) = oldItem == newItem
    }
}
