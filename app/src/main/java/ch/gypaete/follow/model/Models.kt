package ch.gypaete.follow.model

import com.google.gson.annotations.SerializedName

data class ListResponse(
    val ok: Boolean,
    @SerializedName("journee_id") val journeeId: Int,
    @SerializedName("room_code") val roomCode: Int,
    @SerializedName("since_id") val sinceId: Int,
    val vols: List<Vol>
)

data class Vol(
    @SerializedName("vol_id")          val volId: Int,
    @SerializedName("journee_id")      val journeeId: Int,
    @SerializedName("utilisateur_id")  val utilisateurId: Int,
    @SerializedName("nom")             val nomApi: String = "",
    @SerializedName("prenom")          val prenomApi: String = "",
    @SerializedName("pseudo")          val pseudo: String? = null,
    @SerializedName("statut")          val statutApi: String? = null,
    @SerializedName("status")          val statusApi: String? = null,
    @SerializedName("last_event_type") val lastEventType: String? = null,
    @SerializedName("nb_vols_follow")  val nbVols: Int = 0,
    val exercices: List<ExerciceAssigne> = emptyList()
) {
    // Nom complet — l'API peut retourner soit "nom" seul soit "prenom"+"nom"
    val nomComplet: String get() = when {
        prenomApi.isNotBlank() && nomApi.isNotBlank() -> "$prenomApi $nomApi"
        nomApi.isNotBlank() -> nomApi
        else -> "Eleve #$utilisateurId"
    }

    val initiales: String get() = nomComplet
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

    // Statut unifié — l'API peut retourner "statut" (PHP) ou "status" (autre format)
    val statut: String get() {
        val raw = statutApi ?: statusApi ?: ""
        return when (raw) {
            "wait"      -> "en_attente"
            "air"       -> "decolle"
            "landed"    -> "atterri"
            "cancelled" -> "annule"
            else        -> raw.ifEmpty { "en_attente" }
        }
    }

    // Statut UI étendu — tient compte du last_event_type pour Transféré / Arrivée déco
    val statutUI: String get() = when (lastEventType?.uppercase()) {
        "TRANSFERE"        -> "transfere"
        "ARRIVEE_DECO"     -> "arrive_deco"
        "ATTERO_VALIDE_DECO" -> "arrive_deco"
        else               -> statut
    }
}

data class ExerciceAssigne(
    @SerializedName("ex_id") val id: Int = 0,
    val libelle: String = "",
    val categorie: String = "",
    val numero: Int = 0
)

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

data class ActionResponse(
    val ok: Boolean,
    val err: String? = null,
    @SerializedName("max_id") val maxId: Int? = null
)

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
