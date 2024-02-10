import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.bumble.appyx.components.spotlight.Spotlight
import com.bumble.appyx.components.spotlight.SpotlightModel
import com.bumble.appyx.components.spotlight.ui.slider.SpotlightSlider
import com.bumble.appyx.interactions.core.AppyxInteractionsContainer
import com.bumble.appyx.interactions.core.ui.helper.AppyxComponentSetup
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import kotlinx.coroutines.launch
import state.Event
import state.State
import state.StateMachine
import whisper.Segment
import whisper.WhisperRecognizer

object Singleton {
    val whisperRecognizer by lazy {
        WhisperRecognizer()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val statemachine = StateMachine()
    val state by statemachine.state.collectAsState(State.Idle)
    val scope = rememberCoroutineScope()
    Segment.segments.add(Segment("Test", 0, 0))
    val model = remember {
        SpotlightModel<Segment>(
            items = List(1) { Segment.segments[it] },
            initialActiveIndex = 0f,
            savedStateMap = null
        )
    }
    val spotlight =
        Spotlight(
            scope = scope,
            model = model,
            visualisation = { SpotlightSlider(it, model.currentState) },
            gestureFactory = { SpotlightSlider.Gestures(it) }
        )
    AppyxComponentSetup(spotlight)
    var transcription by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    val captureButtonColor = MaterialTheme.colorScheme.error
    val captureButtonColorDefault = MaterialTheme.colorScheme.primaryContainer

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
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppyxInteractionsContainer(
                    appyxComponent = spotlight,
                    screenWidthPx = 420,
                    screenHeightPx = 360,
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = it.interactionTarget.text.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = it.interactionTarget.answer.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(8.dp)
                        )
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
                                captureButtonColor
                            }

                            else -> captureButtonColorDefault
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
        onCloseRequest = ::exitApplication
    ) {
        App()
    }
}
