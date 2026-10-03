package net.spritzlive.identifysong

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.spritzlive.identifysong.audio.AudioRecorder
import net.spritzlive.identifysong.data.HistoryEntry
import net.spritzlive.identifysong.data.HistoryRepository
import net.spritzlive.identifysong.data.toHistoryEntry
import net.spritzlive.identifysong.network.AudDClient
import net.spritzlive.identifysong.network.SongMatch

private const val MAX_ATTEMPTS = 3

sealed interface RecognitionState {
    data object Idle : RecognitionState
    data class Listening(val attempt: Int, val maxAttempts: Int) : RecognitionState
    data class Identifying(val attempt: Int, val maxAttempts: Int) : RecognitionState
    data class Success(val match: SongMatch) : RecognitionState
    data class NoMatch(val message: String) : RecognitionState
    data class Error(val message: String) : RecognitionState
}

class RecognitionViewModel(application: Application) : AndroidViewModel(application) {

    private val historyRepository = HistoryRepository(application)

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    val history: StateFlow<List<HistoryEntry>> = historyRepository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun identify(context: Context) {
        if (_state.value is RecognitionState.Listening || _state.value is RecognitionState.Identifying) return

        if (BuildConfig.AUDD_API_TOKEN.isBlank()) {
            _state.value = RecognitionState.Error(
                "No AudD API token configured. Add AUDD_API_TOKEN to local.properties and rebuild."
            )
            return
        }

        viewModelScope.launch {
            // A single tap's first capture often catches a spoken intro or quiet fade-in that
            // doesn't fingerprint well, so retry a couple of times automatically before giving up.
            for (attempt in 1..MAX_ATTEMPTS) {
                _state.value = RecognitionState.Listening(attempt, MAX_ATTEMPTS)
                val wavFile = try {
                    AudioRecorder.recordWav(context)
                } catch (e: Exception) {
                    _state.value = RecognitionState.Error(e.message ?: "Failed to record audio")
                    return@launch
                }

                _state.value = RecognitionState.Identifying(attempt, MAX_ATTEMPTS)
                try {
                    val match = AudDClient.recognize(BuildConfig.AUDD_API_TOKEN, wavFile)
                    if (match != null) {
                        _state.value = RecognitionState.Success(match)
                        historyRepository.add(match.toHistoryEntry())
                        return@launch
                    }
                    if (attempt == MAX_ATTEMPTS) {
                        _state.value = RecognitionState.NoMatch(
                            "No match found after $MAX_ATTEMPTS tries — try moving closer to the speaker."
                        )
                        return@launch
                    }
                } catch (e: Exception) {
                    _state.value = RecognitionState.Error(e.message ?: "Recognition failed")
                    return@launch
                } finally {
                    wavFile.delete()
                }
            }
        }
    }

    fun reset() {
        _state.value = RecognitionState.Idle
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }
}
