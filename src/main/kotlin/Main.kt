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
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import kotlinx.coroutines.launch
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

    var transcription by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    val captureButtonColor = MaterialTheme.colorScheme.error
    val captureButtonColorDefault = MaterialTheme.colorScheme.primaryContainer
    val whisperRecognizer by lazy {
        WhisperRecognizer()
    }

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
                            onClick = {/*处理设置*/ }
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

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = transcription,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(3f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = answer,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            when (state) {
                                State.Capturing -> {
                                    statemachine.dispatch(Event.Pause)
                                }

                                State.Idle -> statemachine.dispatch(Event.Capture)
                                State.Paused -> statemachine.dispatch(Event.Capture)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    containerColor = when (state) {
                        State.Capturing -> {
                            captureButtonColor
                        }

                        State.Idle -> captureButtonColorDefault
                        State.Paused -> captureButtonColorDefault
                    }
                ) {
                    Icon(
                        imageVector = when (state) {
                            State.Capturing -> {
                                Icons.Default.Pause
                            }

                            State.Idle -> Icons.Default.PlayArrow
                            State.Paused -> Icons.Default.PlayArrow
                        },
                        contentDescription = "Capture"
                    )
                }
                when (state) {
                    is State.Paused, State.Capturing -> {
                        FloatingActionButton(
                            onClick = {
                            },
                            modifier = Modifier
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.Add, "New")
                        }
                    }

                    State.Idle -> TODO()
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
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
