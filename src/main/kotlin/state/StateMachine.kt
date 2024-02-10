package state

import Singleton
import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class StateMachine(initialState: State = State.Idle) : FlowReduxStateMachine<State, Event>(initialState) {

    init {
        spec {
            inState<State.Capturing> {
                onEnter { state ->
                    try {
                        Singleton.audio.start()
                        Singleton.whisperRecognizer.startRecognition()
                        state.noChange()
                    } catch (t: Throwable) {

                        state.override { State.Error("A error occurred") }
                    }
                }

                on<Event.StartNewSession> { _, state ->
                    try {
                        Singleton.audio.clear()
                        Singleton.whisperRecognizer.stopRecognition()
                        state.override { State.Idle }
                    } catch (t: Throwable) {

                        state.override { State.Error("A error occurred") }
                    }
                }
                on<Event.Pause> { _, state ->
                    try {
                        state.override { State.Paused }
                    } catch (t: Throwable) {

                        state.override { State.Error("A error occurred") }
                    }
                }
            }

            inState<State.Paused> {
                onEnter { state ->
                    try {
                        Singleton.audio.pause()
                        Singleton.whisperRecognizer.stopRecognition()
                        state.noChange()
                    } catch (t: Throwable) {

                        state.override { State.Error("A error occurred") }
                    }
                }
                on<Event.StartNewSession> { _, state ->
                    try {
                        Singleton.audio.clear()
                        Singleton.whisperRecognizer.stopRecognition()
                        state.override { State.Idle }
                    } catch (t: Throwable) {

                        state.override { State.Error("A error occurred") }
                    }
                }
                on<Event.Capture> { _, state ->
                    try {
                        state.override { State.Capturing }
                    } catch (t: Throwable) {

                        state.override { State.Error("A error occurred") }
                    }
                }
            }
            inState<State.Idle> {
                on<Event.Capture> { _, state ->
                    try {
                        state.override { State.Capturing }
                    } catch (t: Throwable) {
                        state.override { State.Error("A error occurred") }
                    }
                }
            }
        }
    }
}
