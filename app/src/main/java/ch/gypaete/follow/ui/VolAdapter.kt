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

            val vuDeco = vol.lastEventType?.uppercase() == "ATTERO_VALIDE_DECO"

            val statutLabel = when {
                // MODE DECO : info venant de atterro
                mode == FollowMode.DECO && vol.status == "wait"   -> "En attente"
                mode == FollowMode.DECO && vol.status == "air" && vuDeco -> "VALIDE deco"
                mode == FollowMode.DECO && vol.status == "air"    -> "En l'air"
                mode == FollowMode.DECO && vol.status == "down"   -> "POSE"
                mode == FollowMode.DECO && vol.status == "xfer"   -> "Transfere"
                mode == FollowMode.DECO && vol.status == "cancel" -> "Annule"
                // MODE ATTERRO : info venant du deco
                mode == FollowMode.ATTERRO && vol.status == "air"  -> "En l'air"
                mode == FollowMode.ATTERRO && vol.status == "down" -> "Pose"
                mode == FollowMode.ATTERRO && vol.status == "xfer" -> "Transfere"
                else -> vol.status
            }
            val statutColor = when {
                mode == FollowMode.DECO && vol.status == "air" && vuDeco -> "#2E7D32"
                mode == FollowMode.DECO && vol.status == "down"          -> "#2E7D32"
                vol.status == "wait"   -> "#607D8B"
                vol.status == "air"    -> "#1565C0"
                vol.status == "down"   -> "#2E7D32"
                vol.status == "cancel" -> "#B71C1C"
                vol.status == "xfer"   -> "#6A1B9A"
                else                   -> "#757575"
            }
            tvStatut.text = statutLabel
            tvStatut.setBackgroundColor(Color.parseColor(statutColor))

            // Exercices - utilise code + libelle retournes par API
            val exosValides = vol.exercices.filter { it.libelle.isNotBlank() }
            if (exosValides.isNotEmpty()) {
                tvExos.visibility = View.VISIBLE
                tvExos.text = exosValides.joinToString("\n") { exo ->
                    if (exo.codeExo.isNotBlank()) "${exo.codeExo}  ${exo.libelle}"
                    else exo.libelle
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

            val cardColor = when (vol.status) {
                "air"    -> if (vuDeco) Color.parseColor("#E8F5E9") else Color.parseColor("#E3F2FD")
                "down"   -> Color.parseColor("#E8F5E9")
                "cancel" -> Color.parseColor("#FFEBEE")
                "xfer"   -> Color.parseColor("#F3E5F5")
                else     -> Color.WHITE
            }
            cardView.setCardBackgroundColor(cardColor)

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
