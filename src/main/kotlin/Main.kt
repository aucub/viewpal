import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.bumble.appyx.components.spotlight.Spotlight
import com.bumble.appyx.components.spotlight.SpotlightModel
import com.bumble.appyx.components.spotlight.operation.first
import com.bumble.appyx.components.spotlight.operation.last
import com.bumble.appyx.components.spotlight.operation.next
import com.bumble.appyx.components.spotlight.operation.previous
import com.bumble.appyx.components.spotlight.ui.slider.SpotlightSlider
import com.bumble.appyx.interactions.core.AppyxInteractionsContainer
import com.bumble.appyx.interactions.core.ui.gesture.GestureSettleConfig
import com.bumble.appyx.interactions.core.ui.helper.AppyxComponentSetup
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import kotlinx.coroutines.launch
import state.Event
import state.State
import state.StateMachine
import whisper.Audio
import whisper.Segment
import whisper.WhisperRecognizer
import kotlin.math.roundToInt

object Singleton {
    val whisperRecognizer by lazy {
        WhisperRecognizer()
    }
    var audio: Audio = Audio()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    val statemachine = StateMachine()
    val state by statemachine.state.collectAsState(State.Idle)
    val scope = rememberCoroutineScope()
    Segment.segments.add(Segment("", 0, 0))
    val model = remember {
        SpotlightModel(
            items = Segment.segments,
            initialActiveIndex = (Segment.segments.size - 1).toFloat(),
            savedStateMap = null
        )
    }
    val spotlight = Spotlight(
        scope = scope,
        model = model,
        visualisation = { SpotlightSlider(it, model.currentState) },
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow / 4),
        gestureFactory = {
            SpotlightSlider.Gestures(
                transitionBounds = it,
                orientation = Orientation.Vertical,
                reverseOrientation = true,
            )
        },
        gestureSettleConfig = GestureSettleConfig(
            completionThreshold = 0.2f,
            completeGestureSpec = spring(),
            revertGestureSpec = spring(),
        ),
    )
    val actions = mapOf(
        "First" to { spotlight.first() },
        "Prev" to { spotlight.previous() },
        "Next" to { spotlight.next() },
        "Last" to { spotlight.last() },
    )
    AppyxComponentSetup(spotlight)
    val capturingButtonColor = MaterialTheme.colorScheme.error
    val captureButtonColor = MaterialTheme.colorScheme.primaryContainer

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "viewpal",
                            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.Start),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {}
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Spacer(Modifier.height(16.dp))
                AppyxInteractionsContainer(
                    appyxComponent = spotlight,
                    screenWidthPx = (LocalWindowInfo.current.containerSize.width * LocalDensity.current.density).roundToInt(),
                    screenHeightPx = (LocalWindowInfo.current.containerSize.height * LocalDensity.current.density).roundToInt(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = it.interactionTarget.text.toString(),
                        color = Color.Black,
                    )
                    /*Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = it.interactionTarget.text.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(8.dp),
                            color = Color.Black
                        )
                    }*/
                    /* Card(
                         modifier = Modifier
                             .fillMaxWidth()
                             .padding(16.dp),
                         shape = RoundedCornerShape(8.dp)
                     ) {
                         Text(
                             text = it.interactionTarget.answer.toString(),
                             style = MaterialTheme.typography.titleMedium,
                             modifier = Modifier.padding(8.dp),
                             color = Color.Black
                         )
                     }*/
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions.keys.forEachIndexed { index, key ->
                        Box(
                            modifier = Modifier
                                .clickable { actions.getValue(key).invoke() }
                                .padding(horizontal = 18.dp, vertical = 4.dp)
                        ) {
                            Text(key)
                        }
                        if (index != actions.size - 1) {
                            Spacer(modifier = Modifier.requiredWidth(8.dp))
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (state) {
                        is State.Paused, State.Capturing -> {
                            FloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        statemachine.dispatch(Event.StartNewSession)
                                    }
                                },
                                modifier = Modifier
                                    .padding(start = 16.dp),
                            ) {
                                Icon(Icons.Default.Add, "New")
                            }
                        }

                        else -> {}
                    }
                    Spacer(Modifier.weight(1f, true))
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                when (state) {
                                    State.Capturing -> {
                                        statemachine.dispatch(Event.Pause)
                                    }

                                    State.Idle -> statemachine.dispatch(Event.Capture)
                                    State.Paused -> statemachine.dispatch(Event.Capture)
                                    else -> {}
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 16.dp),
                        containerColor = when (state) {
                            State.Capturing -> {
                                capturingButtonColor
                            }

                            else -> captureButtonColor
                        }
                    ) {
                        Icon(
                            imageVector = when (state) {
                                State.Capturing -> {
                                    Icons.Default.Pause
                                }

                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = when (state) {
                                State.Capturing -> {
                                    "Pause"
                                }

                                else -> "Capture"
                            },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun About() {
    Window(title = "AboutLibraries", onCloseRequest = {}) {
        MaterialTheme {
            Scaffold(
                topBar = { TopAppBar(title = { Text("AboutLibraries") }) }
            ) { it ->
                LibrariesContainer(useResource("aboutlibraries.json") {
                    it.bufferedReader().readText()
                }, Modifier.fillMaxSize().padding(it))
            }
        }
    }
}

fun main() = application {
    Window(
        title = "viewpal",
        icon = painterResource("icon.png"),
        onCloseRequest = ::exitApplication
    ) {
        App()
    }
}
