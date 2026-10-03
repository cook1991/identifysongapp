package net.spritzlive.identifysong

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.spritzlive.identifysong.audio.AudioRecorder
import net.spritzlive.identifysong.network.AudDClient
import net.spritzlive.identifysong.network.SongMatch

sealed interface RecognitionState {
    data object Idle : RecognitionState
    data object Listening : RecognitionState
    data object Identifying : RecognitionState
    data class Success(val match: SongMatch) : RecognitionState
    data class NoMatch(val message: String) : RecognitionState
    data class Error(val message: String) : RecognitionState
}

class RecognitionViewModel : ViewModel() {

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    fun identify(context: Context) {
        if (_state.value is RecognitionState.Listening || _state.value is RecognitionState.Identifying) return

        if (BuildConfig.AUDD_API_TOKEN.isBlank()) {
            _state.value = RecognitionState.Error(
                "No AudD API token configured. Add AUDD_API_TOKEN to local.properties and rebuild."
            )
            return
        }

        viewModelScope.launch {
            _state.value = RecognitionState.Listening
            val wavFile = try {
                AudioRecorder.recordWav(context)
            } catch (e: Exception) {
                _state.value = RecognitionState.Error(e.message ?: "Failed to record audio")
                return@launch
            }

            _state.value = RecognitionState.Identifying
            try {
                val match = AudDClient.recognize(BuildConfig.AUDD_API_TOKEN, wavFile)
                _state.value = if (match != null) {
                    RecognitionState.Success(match)
                } else {
                    RecognitionState.NoMatch("No match found — try again closer to the speaker.")
                }
            } catch (e: Exception) {
                _state.value = RecognitionState.Error(e.message ?: "Recognition failed")
            } finally {
                wavFile.delete()
            }
        }
    }

    fun reset() {
        _state.value = RecognitionState.Idle
    }
}
