package state

sealed class State {
    data object Idle : State()

    data object Capturing : State()

    data object Paused : State()

    data class Error(val message: String) : State()
}

sealed class Event {
    data object Capture : Event()

    data object Pause : Event()

    data object StartNewSession : Event()
}
