package ch.gypaete.follow.ui

import android.app.Application
import androidx.lifecycle.*
import ch.gypaete.follow.api.FollowRepository
import ch.gypaete.follow.model.*
import ch.gypaete.follow.util.SoundManager
import ch.gypaete.follow.util.NotificationHelper
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

class FollowViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FollowRepository()
    private val ctx  = application.applicationContext

    private val _uiState = MutableStateFlow(FollowUiState())
    val uiState: StateFlow<FollowUiState> = _uiState.asStateFlow()

    private val _exercices = MutableStateFlow<List<Exercice>>(emptyList())
    val exercices: StateFlow<List<Exercice>> = _exercices.asStateFlow()

    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    private var pollJob: Job? = null
    private val POLL_INTERVAL_MS = 5_000L

    init {
        NotificationHelper.init(ctx)
        loadExercices()
        refresh()
    }

    fun setMode(mode: FollowMode) { _uiState.update { it.copy(mode = mode) } }

    fun setRoom(room: Int) {
        _uiState.update { it.copy(room = room, sinceId = 0) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = UiStatus.LOADING, errorMsg = null) }
            repo.fetchList(_uiState.value.room)
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

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                doPoll()
            }
        }
    }

    private fun stopPolling() { pollJob?.cancel(); pollJob = null }

    private suspend fun doPoll() {
        val st = _uiState.value
        repo.poll(st.room, st.sinceId)
            .onSuccess { data ->
                if (data.events.isNotEmpty()) {
                    repo.fetchList(st.room).onSuccess { full ->
                        _uiState.update { s ->
                            s.copy(vols = full.vols, sinceId = data.maxId,
                                lastRefresh = System.currentTimeMillis(), isLive = true)
                        }
                    }
                } else {
                    _uiState.update { s -> s.copy(sinceId = data.maxId, isLive = true) }
                }
            }
            .onFailure { _uiState.update { s -> s.copy(isLive = false) } }
    }

    fun decolle(vol: Vol, exerciceIds: List<Int>) {
        viewModelScope.launch {
            repo.decolle(vol.volId, vol.utilisateurId, _uiState.value.room, exerciceIds)
                .onSuccess {
                    SoundManager.playForAction(ctx, "decolle")
                    NotificationHelper.send(ctx, "Decollage", "${vol.nomComplet} a decollé", "decolle")
                    _toast.emit("Decollage : ${vol.nomComplet}")
                    refresh()
                }
                .onFailure { e -> _toast.emit("Erreur : ${e.message}") }
        }
    }

    fun atterri(vol: Vol) {
        viewModelScope.launch {
            repo.atterri(vol.volId, vol.utilisateurId, _uiState.value.room)
                .onSuccess {
                    SoundManager.playForAction(ctx, "atterri")
                    NotificationHelper.send(ctx, "Pose", "${vol.nomComplet} est posé", "atterri")
                    _toast.emit("Pose : ${vol.nomComplet}")
                    refresh()
                }
                .onFailure { e -> _toast.emit("Erreur : ${e.message}") }
        }
    }

    fun annule(vol: Vol) {
        viewModelScope.launch {
            repo.annule(vol.volId, vol.utilisateurId, _uiState.value.room)
                .onSuccess {
                    SoundManager.playForAction(ctx, "annule")
                    NotificationHelper.send(ctx, "Annule", "${vol.nomComplet} annule", "annule")
                    _toast.emit("Annule : ${vol.nomComplet}")
                    refresh()
                }
                .onFailure { e -> _toast.emit("Erreur : ${e.message}") }
        }
    }

    fun transfere(vol: Vol) {
        viewModelScope.launch {
            repo.transfere(vol.volId, vol.utilisateurId, _uiState.value.room)
                .onSuccess {
                    SoundManager.playForAction(ctx, "transfere")
                    NotificationHelper.send(ctx, "Transfere", "${vol.nomComplet} transfere", "transfere")
                    _toast.emit("Transfere : ${vol.nomComplet}")
                    refresh()
                }
                .onFailure { e -> _toast.emit("Erreur : ${e.message}") }
        }
    }

    fun atteroValideDeco(vol: Vol) {
        viewModelScope.launch {
            repo.atteroValideDeco(vol.volId, vol.utilisateurId, _uiState.value.room)
                .onSuccess {
                    SoundManager.playForAction(ctx, "attero_valide_deco")
                    NotificationHelper.send(ctx, "Vu deco", "${vol.nomComplet} vu au deco", "attero_valide_deco")
                    _toast.emit("Vu deco : ${vol.nomComplet}")
                    refresh()
                }
                .onFailure { e -> _toast.emit("Erreur : ${e.message}") }
        }
    }

    fun arriveDeco(vol: Vol) {
        viewModelScope.launch {
            repo.arriveDeco(vol.volId, vol.utilisateurId, _uiState.value.room)
                .onSuccess {
                    SoundManager.playForAction(ctx, "arrive_deco")
                    NotificationHelper.send(ctx, "Arrive deco", "${vol.nomComplet} arrive au deco", "arrive_deco")
                    _toast.emit("Arrive au deco : ${vol.nomComplet}")
                    refresh()
                }
                .onFailure { e -> _toast.emit("Erreur : ${e.message}") }
        }
    }

    private fun loadExercices() {
        viewModelScope.launch {
            repo.fetchExercices()
                .onSuccess { list -> _exercices.update { list } }
        }
    }

    override fun onCleared() { super.onCleared(); stopPolling() }
}
