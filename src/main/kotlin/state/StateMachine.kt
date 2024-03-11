package state

import Singleton
import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class StateMachine(initialState: State = State.Idle) : FlowReduxStateMachine<State, Event>(initialState) {
    private val logger = KotlinLogging.logger {}

    init {
        spec {
            inState<State.Capturing> {
                onEnter { state ->
                    try {
                        Singleton.audio.start()
                        state.noChange()
                    } catch (t: Throwable) {
                        logger.error(t) { t.localizedMessage }
                        state.override { State.Error("A error occurred") }
                    }
                }

                on<Event.StartNewSession> { _, state ->
                    try {
                        Singleton.audio.clear()
                        Segment.clear()
                        state.override { State.Idle }
                    } catch (t: Throwable) {
                        logger.error(t) { t.localizedMessage }
                        state.override { State.Error("A error occurred") }
                    }
                }
                on<Event.Pause> { _, state ->
                    try {
                        state.override { State.Paused }
                    } catch (t: Throwable) {
                        logger.error(t) { t.localizedMessage }
                        state.override { State.Error("A error occurred") }
                    }
                }
            }

            inState<State.Paused> {
                onEnter { state ->
                    try {
                        Singleton.audio.pause()
                        state.noChange()
                    } catch (t: Throwable) {
                        logger.error(t) { t.localizedMessage }
                        state.override { State.Error("A error occurred") }
                    }
                }
                on<Event.StartNewSession> { _, state ->
                    try {
                        Singleton.audio.clear()
                        Segment.clear()
                        state.override { State.Idle }
                    } catch (t: Throwable) {
                        logger.error(t) { t.localizedMessage }
                        state.override { State.Error("A error occurred") }
                    }
                }
                on<Event.Capture> { _, state ->
                    try {
                        Singleton.audio.resume()
                        state.override { State.Capturing }
                    } catch (t: Throwable) {
                        logger.error(t) { t.localizedMessage }
                        state.override { State.Error("A error occurred") }
                    }
                }
            }
            inState<State.Idle> {
                on<Event.Capture> { _, state ->
                    try {
                        state.override { State.Capturing }
                    } catch (t: Throwable) {
                        logger.error(t) { t.localizedMessage }
                        state.override { State.Error("A error occurred") }
                    }
                }
                onEnter { state ->
                    try {
                        state.noChange()
                    } catch (t: Throwable) {
                        logger.error(t) { t.localizedMessage }
                        state.override { State.Error("A error occurred") }
                    }
                }
            }
        }
    }
}
