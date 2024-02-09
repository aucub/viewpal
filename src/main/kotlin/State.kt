import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed class State {
    companion object {
        var isCapturing by mutableStateOf(false)
    }

    data object Idle : State()
    data object Capturing : State()
    data object Paused : State()
}

sealed class Event {
    data object Capture : Event()
    data object Pause : Event()
    data object StartNewSession : Event()
}