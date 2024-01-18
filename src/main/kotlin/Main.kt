import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import whisper.WhisperRecognizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {

    var transcription by remember { mutableStateOf("") }
    val answer by remember { mutableStateOf("") }
    val captureButtonColor = MaterialTheme.colorScheme.error
    val captureButtonColorDefault =MaterialTheme.colorScheme.primaryContainer
    val whisperRecognizer = WhisperRecognizer()

    val listener = object : WhisperRecognizer.RecognitionListener {
        override fun onResult(text: String) {
            transcription += text
        }
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
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
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
                        WhisperRecognizer.isRecording.value = WhisperRecognizer.isRecording.value.not()
                        if (WhisperRecognizer.isRecording.value) {
                            whisperRecognizer.startRecognition(listener)
                        } else {
                            whisperRecognizer.stopRecognition()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    containerColor = if (WhisperRecognizer.isRecording.value) captureButtonColor else captureButtonColorDefault
                ) {
                    Icon(
                        imageVector = if (WhisperRecognizer.isRecording.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Capture"
                    )
                }
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
