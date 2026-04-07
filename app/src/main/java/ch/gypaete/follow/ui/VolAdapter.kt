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
            val lastEvt = vol.lastEventType?.uppercase() ?: ""
            when (mode) {

                FollowMode.DECO -> {
                    when {
                        // Transfere par atterro => bouton Arrive au deco
                        vol.status == "xfer" -> {
                            btnPrimary.text = "Arrive au deco"
                            btnPrimary.setBackgroundColor(Color.parseColor("#1565C0"))
                            btnPrimary.visibility = View.VISIBLE
                            btnPrimary.isEnabled = true
                            btnPrimary.setOnClickListener { onAction(vol, "arrive_deco") }
                            btnSecondary.text = "Annuler"
                            btnSecondary.setBackgroundColor(Color.parseColor("#B71C1C"))
                            btnSecondary.visibility = View.VISIBLE
                            btnSecondary.isEnabled = true
                            btnSecondary.setOnClickListener { onAction(vol, "annule") }
                        }
                        // En attente => Decollage + Annuler
                        vol.statut == "en_attente" -> {
                            btnPrimary.text = "Decollage"
                            btnPrimary.setBackgroundColor(Color.parseColor("#1565C0"))
                            btnPrimary.visibility = View.VISIBLE
                            btnPrimary.isEnabled = true
                            btnPrimary.setOnClickListener { onAction(vol, "decolle") }
                            btnSecondary.text = "Annuler"
                            btnSecondary.setBackgroundColor(Color.parseColor("#B71C1C"))
                            btnSecondary.visibility = View.VISIBLE
                            btnSecondary.isEnabled = true
                            btnSecondary.setOnClickListener { onAction(vol, "annule") }
                        }
                        // En l'air => boutons caches
                        else -> {
                            btnPrimary.visibility = View.GONE
                            btnSecondary.visibility = View.GONE
                        }
                    }
                }

                FollowMode.ATTERRO -> {
                    when {
                        // En l'air => Vu deco + Pose actif + Transfere desactive
                        vol.statut == "decolle" -> {
                            val vuDecoFait = lastEvt == "ATTERO_VALIDE_DECO"
                            // Bouton Vu deco (seulement si pas encore fait)
                            if (!vuDecoFait) {
                                btnPrimary.text = "Vu deco"
                                btnPrimary.setBackgroundColor(Color.parseColor("#1565C0"))
                                btnPrimary.visibility = View.VISIBLE
                                btnPrimary.isEnabled = true
                                btnPrimary.setOnClickListener { onAction(vol, "attero_valide_deco") }
                            } else {
                                // Vu deco fait => Pose
                                btnPrimary.text = "Pose"
                                btnPrimary.setBackgroundColor(Color.parseColor("#2E7D32"))
                                btnPrimary.visibility = View.VISIBLE
                                btnPrimary.isEnabled = true
                                btnPrimary.setOnClickListener { onAction(vol, "atterri") }
                            }
                            // Transfere desactive
                            btnSecondary.text = "Transfere"
                            btnSecondary.setBackgroundColor(Color.parseColor("#9E9E9E"))
                            btnSecondary.visibility = View.VISIBLE
                            btnSecondary.isEnabled = false
                            btnSecondary.setOnClickListener(null)
                        }
                        // Pose => Pose desactive + Transfere actif
                        vol.statut == "atterri" -> {
                            btnPrimary.text = "Pose"
                            btnPrimary.setBackgroundColor(Color.parseColor("#9E9E9E"))
                            btnPrimary.visibility = View.VISIBLE
                            btnPrimary.isEnabled = false
                            btnPrimary.setOnClickListener(null)
                            btnSecondary.text = "Transfere"
                            btnSecondary.setBackgroundColor(Color.parseColor("#6A1B9A"))
                            btnSecondary.visibility = View.VISIBLE
                            btnSecondary.isEnabled = true
                            btnSecondary.setOnClickListener { onAction(vol, "transfere") }
                        }
                        // Autres => caches
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
