package ch.gypaete.follow.ui

import androidx.lifecycle.*
import ch.gypaete.follow.api.FollowRepository
import ch.gypaete.follow.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class FollowMode { DECO, ATTERRO }
enum class UiStatus   { IDLE, LOADING, ERROR, SUCCESS }

data class FollowUiState(
    val status: UiStatus = UiStatus.IDLE,
    val vols: List<Vol> = emptyList(),
    val mode: FollowMode = FollowMode.DECO,
    val room: Int = 1,
    val journeeId: Int = 0,
    val sinceId: Int = 0,
    val isLive: Boolean = false,
    val errorMsg: String? = null,
    val lastRefresh: Long = 0L
)

class FollowViewModel : ViewModel() {

    private val repo = FollowRepository()

    private val _uiState = MutableStateFlow(FollowUiState())
    val uiState: StateFlow<FollowUiState> = _uiState.asStateFlow()

    // ── Exercices FSVL (chargés une fois) ───────────────────────────────
    private val _exercices = MutableStateFlow<List<Exercice>>(emptyList())
    val exercices: StateFlow<List<Exercice>> = _exercices.asStateFlow()

    // ── Toast one-shot ────────────────────────────────────────────────────
    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    private var pollJob: Job? = null
    private val POLL_INTERVAL_MS = 5_000L

    // ── Init ─────────────────────────────────────────────────────────────
    init {
        loadExercices()
        refresh()
    }

    // ── Changement de mode (Déco ↔ Atterro) ─────────────────────────────
    fun setMode(mode: FollowMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    // ── Changement de room ────────────────────────────────────────────────
    fun setRoom(room: Int) {
        _uiState.update { it.copy(room = room, sinceId = 0) }
        refresh()
    }

    // ── Chargement initial / forcer refresh ───────────────────────────────
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = UiStatus.LOADING, errorMsg = null) }
            val room = _uiState.value.room
            repo.fetchList(room)
                .onSuccess { data ->
                    _uiState.update { st ->
                        st.copy(
                            status = UiStatus.SUCCESS,
                            vols = data.vols,
                            journeeId = data.journeeId,
                            sinceId = data.sinceId,
                            lastRefresh = System.currentTimeMillis(),
                            isLive = true
                        )
                    }
                    startPolling()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(status = UiStatus.ERROR, errorMsg = e.message, isLive = false) }
                    stopPolling()
                }
        }
    }

    // ── Polling ───────────────────────────────────────────────────────────
    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                doPoll()
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun doPoll() {
        val st = _uiState.value
        repo.poll(st.room, st.sinceId)
            .onSuccess { data ->
                if (data.events.isNotEmpty()) {
                    // Il y a du nouveau → rechargement complet de la liste
                    repo.fetchList(st.room).onSuccess { full ->
                        _uiState.update { s ->
                            s.copy(
                                vols = full.vols,
                                sinceId = data.maxId,
                                lastRefresh = System.currentTimeMillis(),
                                isLive = true
                            )
                        }
                    }
                } else {
                    _uiState.update { s -> s.copy(sinceId = data.maxId, isLive = true) }
                }
            }
            .onFailure {
                _uiState.update { s -> s.copy(isLive = false) }
            }
    }

    // ── Actions ───────────────────────────────────────────────────────────
    fun decolle(vol: Vol, exerciceIds: List<Int>) {
        viewModelScope.launch {
            val room = _uiState.value.room
            repo.decolle(vol.volId, vol.utilisateurId, room, exerciceIds)
                .onSuccess {
                    _toast.emit("✈ ${vol.prenom} — décollage enregistré")
                    refresh()
                }
                .onFailure { e ->
                    _toast.emit("Erreur : ${e.message}")
                }
        }
    }

    fun atterri(vol: Vol) {
        viewModelScope.launch {
            val room = _uiState.value.room
            repo.atterri(vol.volId, vol.utilisateurId, room)
                .onSuccess {
                    _toast.emit("✅ ${vol.prenom} — posé")
                    refresh()
                }
                .onFailure { e ->
                    _toast.emit("Erreur : ${e.message}")
                }
        }
    }

    fun annule(vol: Vol) {
        viewModelScope.launch {
            val room = _uiState.value.room
            repo.annule(vol.volId, vol.utilisateurId, room)
                .onSuccess {
                    _toast.emit("↩ ${vol.prenom} — annulé")
                    refresh()
                }
                .onFailure { e ->
                    _toast.emit("Erreur : ${e.message}")
                }
        }
    }

    fun transfere(vol: Vol) {
        viewModelScope.launch {
            val room = _uiState.value.room
            repo.transfere(vol.volId, vol.utilisateurId, room)
                .onSuccess {
                    _toast.emit("🔁 ${vol.prenom} — transféré")
                    refresh()
                }
                .onFailure { e ->
                    _toast.emit("Erreur : ${e.message}")
                }
        }
    }

    fun arriveDeco(vol: Vol) {
        viewModelScope.launch {
            val room = _uiState.value.room
            repo.arriveDeco(vol.volId, vol.utilisateurId, room)
                .onSuccess {
                    _toast.emit("⛰ ${vol.prenom} — arrivé au déco")
                    refresh()
                }
                .onFailure { e ->
                    _toast.emit("Erreur : ${e.message}")
                }
        }
    }

    // ── Exercices ─────────────────────────────────────────────────────────
    private fun loadExercices() {
        viewModelScope.launch {
            repo.fetchExercices()
                .onSuccess { list -> _exercices.update { list } }
                .onFailure { /* silencieux — on réessaiera */ }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
