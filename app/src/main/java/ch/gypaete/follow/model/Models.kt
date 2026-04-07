package ch.gypaete.follow.model

import com.google.gson.annotations.SerializedName

// ── Réponse GET list ────────────────────────────────────────────────────────
data class ListResponse(
    val ok: Boolean,
    @SerializedName("journee_id") val journeeId: Int,
    @SerializedName("room_code") val roomCode: Int,
    @SerializedName("since_id") val sinceId: Int,
    val vols: List<Vol>
)

// ── Vol (un élève dans la journée) ──────────────────────────────────────────
data class Vol(
    @SerializedName("vol_id")       val volId: Int,
    @SerializedName("utilisateur_id") val utilisateurId: Int,
    val prenom: String,
    val nom: String,
    val statut: String,          // en_attente | decolle | atterri | annule
    @SerializedName("decollage_at")     val decollageAt: String?,
    @SerializedName("atterrissage_at")  val atterrissageAt: String?,
    val exercices: List<ExerciceAssigne> = emptyList()
) {
    val nomComplet get() = "$prenom $nom"
    val initiales get() = "${prenom.firstOrNull() ?: ""}${nom.firstOrNull() ?: ""}".uppercase()
}

data class ExerciceAssigne(
    val id: Int,
    val libelle: String,
    val categorie: String,
    val numero: Int
)

// ── Exercices FSVL (pour la sélection au décollage) ─────────────────────────
data class ExercicesResponse(
    val ok: Boolean,
    val exercices: List<Exercice>
)

data class Exercice(
    val id: Int,
    val categorie: String,   // NF1 | NF2 | THEO
    val numero: Int,
    val libelle: String
)

// ── Réponse POST actions ─────────────────────────────────────────────────────
data class ActionResponse(
    val ok: Boolean,
    val err: String? = null,
    @SerializedName("max_id") val maxId: Int? = null
)

// ── Réponse GET poll ─────────────────────────────────────────────────────────
data class PollResponse(
    val ok: Boolean,
    val events: List<Event>,
    @SerializedName("max_id") val maxId: Int
)

data class Event(
    val id: Int,
    @SerializedName("vol_id") val volId: Int,
    val type: String,
    @SerializedName("utilisateur_id") val utilisateurId: Int,
    @SerializedName("created_at") val createdAt: String
)

// ── Rooms ────────────────────────────────────────────────────────────────────
data class Room(
    @SerializedName("room_code") val roomCode: Int,
    val libelle: String
)
