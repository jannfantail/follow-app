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
    @SerializedName("vol_id")        val volId: Int,
    @SerializedName("journee_id")    val journeeId: Int,
    @SerializedName("utilisateur_id") val utilisateurId: Int,
    @SerializedName("nom")           val nomComplet: String,   // API retourne nom complet
    @SerializedName("pseudo")        val pseudo: String?,
    @SerializedName("status")        val status: String,       // wait | air | landed | cancelled
    @SerializedName("last_event_type") val lastEventType: String?,
    @SerializedName("nb_vols_follow") val nbVols: Int,
    val exercices: List<ExerciceAssigne> = emptyList()
) {
    // Compatibilité avec le reste du code
    val statut get() = when (status) {
        "wait"      -> "en_attente"
        "air"       -> "decolle"
        "landed"    -> "atterri"
        "cancelled" -> "annule"
        else        -> status
    }

    val prenom get() = nomComplet.split(" ").firstOrNull() ?: nomComplet
    val nom get()    = nomComplet.split(" ").drop(1).joinToString(" ").ifEmpty { nomComplet }

    val initiales get() = nomComplet
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
}

data class ExerciceAssigne(
    val id: Int,
    @com.google.gson.annotations.SerializedName("code") val codeExo: String = "",
    val libelle: String = "",
    val categorie: String = "",
    val numero: Int = 0
)

// ── Exercices FSVL ───────────────────────────────────────────────────────────
data class ExercicesResponse(
    val ok: Boolean,
    val exercices: List<Exercice>
)

data class Exercice(
    val id: Int,
    val categorie: String,
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

data class Room(
    @SerializedName("room_code") val roomCode: Int,
    val libelle: String
)
