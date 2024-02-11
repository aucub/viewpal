import Singleton.statemachine
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.softartdev.theme.material3.PreferableMaterialTheme
import com.softartdev.theme.material3.SettingsScaffold
import com.softartdev.theme.material3.ThemePreferenceItem
import com.softartdev.theme.material3.ThemePreferencesCategory
import config.Config
import config.Config.Companion.whisperConfig
import dev.langchain4j.model.openai.OpenAiChatModelName
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
    val statemachine = StateMachine()
    var showAboutWindow by mutableStateOf(false)
    var showSettingsWindow by mutableStateOf(false)
    var showPreferableDialog by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    val coroutineScope = rememberCoroutineScope()
    val state by statemachine.state.collectAsState(State.Idle)
    Segment.init()
    val model = SpotlightModel(
        items = Segment.segments,
        initialActiveIndex = (Segment.segments.size - 1).toFloat(),
        savedStateMap = null
    )
    val spotlight = Spotlight(
        scope = coroutineScope,
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
        "Last" to {
            spotlight.last()
        },
    )
    AppyxComponentSetup(spotlight)
    val capturingButtonColor = MaterialTheme.colorScheme.error
    val captureButtonColor = MaterialTheme.colorScheme.primaryContainer

    PreferableMaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "viewpal",
                            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.Start),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                Singleton.showPreferableDialog = true
                            }
                        ) {
                            Icon(
                                Icons.Default.Brightness4,
                                contentDescription = "PreferableTheme",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(
                            onClick = {
                                Singleton.showAboutWindow = true
                            }
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(
                            onClick = {
                                Singleton.showSettingsWindow = true
                            }
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = Segment.segments.last().text.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                AppyxInteractionsContainer(
                    appyxComponent = spotlight,
                    screenWidthPx = (LocalWindowInfo.current.containerSize.width * LocalDensity.current.density).roundToInt(),
                    screenHeightPx = (LocalWindowInfo.current.containerSize.height * LocalDensity.current.density).roundToInt(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = it.interactionTarget.prompt.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = it.interactionTarget.answer.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions.keys.forEachIndexed { index, key ->
                        FilledTonalButton(
                            onClick = { actions.getValue(key).invoke() },
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                key,
                                color = MaterialTheme.colorScheme.onBackground
                            )
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
                                    coroutineScope.launch {
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
                            coroutineScope.launch {
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
    Window(title = "AboutLibraries", onCloseRequest = { Singleton.showAboutWindow = false }) {
        PreferableMaterialTheme {
            Scaffold(
                topBar = {
                    TopAppBar(title = {
                        Text(
                            "AboutLibraries",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    })
                }
            ) { it ->
                LibrariesContainer(useResource("aboutlibraries.json") {
                    it.bufferedReader().readText()
                }, Modifier.fillMaxSize().padding(it))
            }
        }
    }
}

@Composable
fun Settings() {
    Window(title = "Settings", onCloseRequest = { Singleton.showSettingsWindow = false }) {
        PreferableMaterialTheme {
            Scaffold { _ ->
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "OpenAI Config",
                        modifier = Modifier.padding(vertical = 10.dp),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    // OpenAI基础URL
                    TextField(
                        value = Config.config.openAiBaseUrl,
                        onValueChange = { Config.config.openAiBaseUrl = it },
                        label = {
                            Text(
                                "OpenAI Base URL",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                    // OpenAI API密钥
                    TextField(
                        value = Config.config.openAiApiKey ?: "",
                        onValueChange = { Config.config.openAiApiKey = it },
                        label = {
                            Text(
                                "OpenAI API Key",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                    // 提示模板
                    TextField(
                        value = Config.config.promptTemplate ?: "",
                        onValueChange = { Config.config.promptTemplate = it },
                        label = {
                            Text(
                                "Prompt Template",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                    // 最大令牌数
                    TextField(
                        value = Config.config.maxTokens.toString(),
                        onValueChange = { Config.config.maxTokens = it.toIntOrNull() ?: Config.config.maxTokens },
                        label = {
                            Text(
                                "Max Tokens",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                    var temperature by remember { mutableStateOf(Config.config.temperature) }
                    val temperatureRange = 0.0..1.0
                    val step = 0.1

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Temperature", modifier = Modifier.width(160.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // 减号按钮
                        FilledTonalButton(
                            onClick = { if (temperature - step >= temperatureRange.start) temperature -= step },
                            enabled = temperature > temperatureRange.start,
                        ) {
                            Text(
                                "-",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // 数值展示
                        Text(
                            text = String.format("%.1f", temperature),
                            modifier = Modifier.width(64.dp).padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // 加号按钮
                        FilledTonalButton(
                            onClick = { if (temperature + step <= temperatureRange.endInclusive) temperature += step },
                            enabled = temperature < temperatureRange.endInclusive,
                        ) {
                            Text(
                                "+",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    // 监听temperature变化并更新到Config当中
                    LaunchedEffect(temperature) {
                        Config.config.temperature = temperature
                    }

                    var expanded by remember { mutableStateOf(false) }
                    var selectedIndex by remember { mutableStateOf(0) }
                    val models = OpenAiChatModelName.entries.toTypedArray()
                    val interactionSource = remember { MutableInteractionSource() }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = models[selectedIndex].name,
                            onValueChange = { },
                            label = {
                                Text(
                                    "Preferred Model",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    "Expand dropdown menu",
                                    Modifier.clickable { expanded = true },
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                        )

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            models.forEachIndexed { index, model ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            model.name,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    },
                                    onClick = {
                                        selectedIndex = index
                                        Config.config.preferredModel = model.name
                                        expanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = null,
                                    trailingIcon = null,
                                    enabled = true,
                                    colors = MenuDefaults.itemColors(),
                                    contentPadding = MenuDefaults.DropdownMenuItemContentPadding,
                                    interactionSource = interactionSource
                                )
                            }
                        }
                    }
                    // 主题
                    TextField(
                        value = Config.config.topic ?: "",
                        onValueChange = { Config.config.topic = it },
                        label = {
                            Text(
                                "Topic",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                    // Whisper设置的标题
                    Text(
                        "Whisper Config",
                        modifier = Modifier.padding(vertical = 10.dp),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    // 线程数
                    TextField(
                        value = whisperConfig.nThreads.toString(),
                        onValueChange = { whisperConfig.nThreads = it.toIntOrNull() ?: whisperConfig.nThreads },
                        label = {
                            Text(
                                "Number of Threads",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                    // Step MS
                    TextField(
                        value = whisperConfig.stepMs.toString(),
                        onValueChange = { whisperConfig.stepMs = it.toIntOrNull() ?: whisperConfig.stepMs },
                        label = {
                            Text(
                                "Step (ms)",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )

                    // Length MS
                    TextField(
                        value = whisperConfig.lengthMs.toString(),
                        onValueChange = { whisperConfig.lengthMs = it.toIntOrNull() ?: whisperConfig.lengthMs },
                        label = {
                            Text(
                                "Length (ms)",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )

                    TextField(
                        value = whisperConfig.keepMs.toString(),
                        onValueChange = { whisperConfig.keepMs = it.toIntOrNull() ?: whisperConfig.keepMs },
                        label = {
                            Text(
                                "Keep (ms)",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )

                    TextField(
                        value = whisperConfig.delayMs.toString(),
                        onValueChange = { whisperConfig.delayMs = it.toLongOrNull() ?: whisperConfig.delayMs },
                        label = {
                            Text(
                                "Delay (ms)",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                    // 翻译功能开关
                    RowOptionSwitch(
                        label = "Translate",
                        isChecked = whisperConfig.translate,
                        onCheckedChange = { whisperConfig.translate = it }
                    )

                    // 语言检测开关
                    RowOptionSwitch(
                        label = "DetectLanguage",
                        isChecked = whisperConfig.detectLanguage,
                        onCheckedChange = { whisperConfig.detectLanguage = it }
                    )

                    TextField(
                        value = whisperConfig.language,
                        onValueChange = { whisperConfig.language = it },
                        label = {
                            Text(
                                "Language",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )

                    // 初始提示文本
                    OutlinedTextField(
                        value = whisperConfig.initialPrompt ?: "",
                        onValueChange = { whisperConfig.initialPrompt = it },
                        label = {
                            Text(
                                "初始提示",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 上下文开关
                    RowOptionSwitch(
                        label = "禁用上下文",
                        isChecked = whisperConfig.noContext,
                        onCheckedChange = { whisperConfig.noContext = it }
                    )

                    // GPU 使用开关
                    RowOptionSwitch(
                        label = "使用 GPU",
                        isChecked = whisperConfig.useGPU,
                        onCheckedChange = { whisperConfig.useGPU = it }
                    )
                    var showWhisperLibPicker by remember { mutableStateOf(false) }
                    var showModelPicker by remember { mutableStateOf(false) }

                    FilePicker(showWhisperLibPicker, fileExtensions = listOf("so", "dylib")) { file ->
                        whisperConfig.whisperLib = file?.path ?: whisperConfig.whisperLib
                        showWhisperLibPicker = false
                    }

                    FilePicker(showModelPicker, fileExtensions = listOf("bin")) { file ->
                        whisperConfig.model = file?.path ?: whisperConfig.model
                        showModelPicker = false
                    }

                    Column {
                        Text(
                            "Whisper Library",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        OutlinedTextField(
                            value = whisperConfig.whisperLib,
                            onValueChange = { whisperConfig.whisperLib = it },
                            label = {
                                Text(
                                    "Whisper Library Path",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        )
                        FilledTonalButton(
                            onClick = {
                                showWhisperLibPicker = true
                            },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Select Whisper Library",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Text(
                            "Model Binary",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        OutlinedTextField(
                            value = whisperConfig.model,
                            onValueChange = { whisperConfig.model = it },
                            label = {
                                Text(
                                    "Model Binary Path",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        )
                        FilledTonalButton(
                            onClick = {
                                showModelPicker = true
                            },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Select Model Binary",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }

}


@Composable
fun RowOptionSwitch(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun PreferableDialog(onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            PreferableMaterialTheme { // provides composition locals
                SettingsScaffold { // includes TopAppBar
                    Box {
                        Column {
                            ThemePreferencesCategory() // subtitle
                            ThemePreferenceItem() // menu item
                        }
                        themePrefs.showDialogIfNeed() // shows when menu item clicked
                    }
                }
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
        if (Singleton.showAboutWindow) {
            About()
        }
        if (Singleton.showSettingsWindow) {
            Settings()
        }
        if (Singleton.showPreferableDialog) {
            PreferableDialog { Singleton.showPreferableDialog = false }
        }
    }
}
