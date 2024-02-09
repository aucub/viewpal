import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class StateMachine(initialState: State = State.Idle) : FlowReduxStateMachine<State, Event>(initialState) {

    init {
        spec {
            inState<State.Idle> {
                on<Event.Capture> { _, state ->
                    try {
                        Singleton.whisperRecognizer.startRecognition()
                        state.override {
                            State.Capturing
                        }
                    } catch (t: Throwable) {
                        state.override {
                            State.Idle
                        }
                    }
                }
            }

            inState<State.Capturing> {
                on<Event.Pause> { _, state ->
                    try {
                        Singleton.whisperRecognizer.stopRecognition()
                        state.override {
                            State.Paused
                        }
                    } catch (t: Throwable) {
                        state.override {
                            State.Capturing
                        }
                    }
                }
            }

            inState<State.Paused> {
                on<Event.Capture> { _, state ->
                    try {
                        Singleton.whisperRecognizer.startRecognition()
                        state.override {
                            State.Capturing
                        }
                    } catch (t: Throwable) {
                        state.override {
                            State.Paused
                        }
                    }
                }
            }
        }
    }
}
