package ch.gypaete.follow.api

import ch.gypaete.follow.model.*
import okhttp3.FormBody

class FollowRepository {

    private val api get() = ApiClient.service

    // ── Charge la liste complète des vols ─────────────────────────────────
    suspend fun fetchList(room: Int): Result<ListResponse> = runCatching {
        val resp = api.list(room = room)
        if (!resp.isSuccessful) error("HTTP ${resp.code()}")
        resp.body() ?: error("Réponse vide")
    }

    // ── Poll incrémental ──────────────────────────────────────────────────
    suspend fun poll(room: Int, sinceId: Int): Result<PollResponse> = runCatching {
        val resp = api.poll(room = room, sinceId = sinceId)
        if (!resp.isSuccessful) error("HTTP ${resp.code()}")
        resp.body() ?: error("Réponse vide")
    }

    // ── Rooms ────────────────────────────────────────────────────────────
    suspend fun fetchRooms(): Result<List<ch.gypaete.follow.model.Room>> = runCatching {
        val resp = api.listRooms()
        if (!resp.isSuccessful) error("HTTP ${resp.code()}")
        resp.body()?.rooms ?: listOf(ch.gypaete.follow.model.Room(roomCode = 1, libelle = "Room 1"))
    }

    // ── Exercices FSVL ────────────────────────────────────────────────────
    suspend fun fetchExercices(): Result<List<Exercice>> = runCatching {
        val resp = api.listExercices()
        if (!resp.isSuccessful) error("HTTP ${resp.code()}")
        resp.body()?.exercices ?: error("Réponse vide")
    }

    // ── Décolle avec exercices ────────────────────────────────────────────
    suspend fun decolle(
        volId: Int,
        utilisateurId: Int,
        room: Int,
        exerciceIds: List<Int>
    ): Result<ActionResponse> = runCatching {
        val builder = FormBody.Builder()
            .add("action", "decolle")
            .add("vol_id", volId.toString())
            .add("utilisateur_id", utilisateurId.toString())
            .add("room", room.toString())

        exerciceIds.forEach { id ->
            builder.add("exercices[]", id.toString())
        }

        val resp = api.actionRaw(builder.build())
        if (!resp.isSuccessful) error("HTTP ${resp.code()}")
        val body = resp.body() ?: error("Réponse vide")
        if (!body.ok) error(body.err ?: "Erreur serveur")
        body
    }

    // ── Atterri ──────────────────────────────────────────────────────────
    suspend fun atterri(volId: Int, utilisateurId: Int, room: Int): Result<ActionResponse> =
        postAction("atterri", volId, utilisateurId, room)

    // ── Annule ───────────────────────────────────────────────────────────
    suspend fun annule(volId: Int, utilisateurId: Int, room: Int): Result<ActionResponse> =
        postAction("annule", volId, utilisateurId, room)

    // ── Transféré ────────────────────────────────────────────────────────
    suspend fun transfere(volId: Int, utilisateurId: Int, room: Int): Result<ActionResponse> =
        postAction("transfere", volId, utilisateurId, room)

    // ── Arrivee deco ─────────────────────────────────────────────────────
    suspend fun arriveDeco(volId: Int, utilisateurId: Int, room: Int): Result<ActionResponse> =
        postAction("arrive_deco", volId, utilisateurId, room)

    // ── Atterro valide deco ───────────────────────────────────────────────
    suspend fun atteroValideDeco(volId: Int, utilisateurId: Int, room: Int): Result<ActionResponse> =
        postAction("attero_valide_deco", volId, utilisateurId, room)

    private suspend fun postAction(
        action: String,
        volId: Int,
        utilisateurId: Int,
        room: Int
    ): Result<ActionResponse> = runCatching {
        val resp = api.action(action, volId, utilisateurId, room)
        if (!resp.isSuccessful) error("HTTP ${resp.code()}")
        val body = resp.body() ?: error("Réponse vide")
        if (!body.ok) error(body.err ?: "Erreur serveur")
        body
    }
}
