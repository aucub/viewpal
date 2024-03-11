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
import androidx.compose.material.DropdownMenuItem
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
import asr.Audio
import cafe.adriel.lyricist.strings
import com.bumble.appyx.components.spotlight.Spotlight
import com.bumble.appyx.components.spotlight.SpotlightModel
import com.bumble.appyx.components.spotlight.ui.slider.SpotlightSlider
import com.bumble.appyx.interactions.core.AppyxInteractionsContainer
import com.bumble.appyx.interactions.core.ui.gesture.GestureSettleConfig
import com.bumble.appyx.interactions.core.ui.helper.AppyxComponentSetup
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.markdown.m3.Markdown
import com.softartdev.theme.material3.PreferableMaterialTheme
import com.softartdev.theme.material3.SettingsScaffold
import com.softartdev.theme.material3.ThemePreferenceItem
import com.softartdev.theme.material3.ThemePreferencesCategory
import config.Config
import config.ConfigManager
import dev.langchain4j.model.openai.OpenAiChatModelName
import kotlinx.coroutines.launch
import state.Event
import state.Segment
import state.State
import state.StateMachine
import kotlin.math.roundToInt

object Singleton {
    var audio: Audio = Audio()
    val statemachine = StateMachine()
    var showAboutWindow by mutableStateOf(false)
    var showSettingsWindow by mutableStateOf(false)
    var showPreferableMaterialThemeDialog by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    val coroutineScope = rememberCoroutineScope()
    val state by statemachine.state.collectAsState(State.Idle)
    val model =
        SpotlightModel(
            items = Segment.segments,
            initialActiveIndex = (Segment.segments.size - 1).toFloat(),
            savedStateMap = null,
        )
    val spotlight =
        Spotlight(
            scope = coroutineScope,
            model = model,
            visualisation = { SpotlightSlider(it, model.currentState) },
            animationSpec = spring(stiffness = Spring.StiffnessVeryLow / 4),
            gestureFactory = {
                SpotlightSlider.Gestures(
                    transitionBounds = it,
                    orientation = Orientation.Horizontal, // 修改为水平方向
                    reverseOrientation = true,
                )
            },
            gestureSettleConfig =
            GestureSettleConfig(
                completionThreshold = 0.2f,
                completeGestureSpec = spring(),
                revertGestureSpec = spring(),
            ),
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
                            strings.appTitle,
                            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.Start),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                Singleton.showPreferableMaterialThemeDialog = true
                            },
                        ) {
                            Icon(
                                Icons.Default.Brightness4,
                                contentDescription = strings.contentDescriptionStrings.preferableTheme,
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        IconButton(
                            onClick = {
                                Singleton.showAboutWindow = true
                            },
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = strings.contentDescriptionStrings.info,
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        IconButton(
                            onClick = {
                                Singleton.showSettingsWindow = true
                            },
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = strings.contentDescriptionStrings.settings,
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceAround,
            ) {
                Card(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = if (Segment.asrTexts.isEmpty()) "" else Segment.asrTexts.last().toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                AppyxInteractionsContainer(
                    appyxComponent = spotlight,
                    screenWidthPx = (LocalWindowInfo.current.containerSize.width * LocalDensity.current.density).roundToInt(),
                    screenHeightPx = (LocalWindowInfo.current.containerSize.height * LocalDensity.current.density).roundToInt(),
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Card(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = it.interactionTarget.prompt.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Card(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Markdown(it.interactionTarget.answer.toString())
                        }
                    }
                }
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (state) {
                        is State.Paused, State.Capturing -> {
                            FloatingActionButton(
                                onClick = {
                                    coroutineScope.launch {
                                        statemachine.dispatch(Event.StartNewSession)
                                    }
                                },
                                modifier =
                                Modifier
                                    .padding(start = 16.dp),
                            ) {
                                Icon(Icons.Default.Add, strings.contentDescriptionStrings.startNewSession)
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
                        containerColor =
                        when (state) {
                            State.Capturing -> {
                                capturingButtonColor
                            }

                            else -> captureButtonColor
                        },
                    ) {
                        Icon(
                            imageVector =
                            when (state) {
                                State.Capturing -> {
                                    Icons.Default.Pause
                                }

                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription =
                            when (state) {
                                State.Capturing -> {
                                    strings.contentDescriptionStrings.pause
                                }

                                else -> strings.contentDescriptionStrings.capture
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
    Window(title = strings.aboutTitle, onCloseRequest = { Singleton.showAboutWindow = false }) {
        PreferableMaterialTheme {
            Scaffold(
                topBar = {
                    TopAppBar(title = {
                        Text(
                            strings.aboutTitle,
                            modifier = Modifier.padding(vertical = 10.dp),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    })
                },
            ) { it ->
                LibrariesContainer(
                    useResource("aboutlibraries.json") {
                        it.bufferedReader().readText()
                    },
                    Modifier.fillMaxSize().padding(it),
                )
            }
        }
    }
}

@Composable
fun Settings() {
    var audioDeviceName by remember { mutableStateOf(Config.config.audioConfig.audioDeviceName) }
    var lengthMs by remember { mutableStateOf(Config.config.audioConfig.lengthMs) }
    var delayMs by remember { mutableStateOf(Config.config.audioConfig.delayMs) }
    var translate by remember { mutableStateOf(Config.config.audioConfig.translate) }
    var language by remember { mutableStateOf(Config.config.audioConfig.language) }
    var openAiBaseUrl by remember { mutableStateOf(Config.config.openAiConfig.openAiBaseUrl) }
    var openAiApiKey by remember { mutableStateOf(Config.config.openAiConfig.openAiApiKey ?: "") }
    var systemPrompt by remember { mutableStateOf(Config.config.openAiConfig.systemPrompt ?: "") }
    var maxTokens by remember { mutableStateOf(Config.config.openAiConfig.maxTokens) }
    var temperature by remember { mutableStateOf(Config.config.openAiConfig.temperature) }
    var preferredModel by remember { mutableStateOf(Config.config.openAiConfig.preferredModel) }
    var accountId by remember { mutableStateOf(Config.config.workersAiConfig.accountId) }
    var apiToken by remember { mutableStateOf(Config.config.workersAiConfig.apiToken) }
    Window(title = strings.settingsTitle, onCloseRequest = { Singleton.showSettingsWindow = false }) {
        PreferableMaterialTheme {
            Scaffold { _ ->
                val scrollState = rememberScrollState()
                Column(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SettingsSectionTitle(strings.settingsStrings.audioSettingsStrings.audioSettingsTitle)
                    var audioDeviceExpanded by remember { mutableStateOf(false) }
                    var audioDeviceSelectedIndex by remember { mutableStateOf(0) }
                    val audioDeviceList = Audio.getDeviceList().toMutableList()
                    val defaultAudioDevice = strings.settingsStrings.audioSettingsStrings.audioDeviceDefault
                    audioDeviceList.add(defaultAudioDevice)
                    val audioDeviceInteractionSource = remember { MutableInteractionSource() }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = (if (audioDeviceName.isNullOrEmpty()) strings.settingsStrings.audioSettingsStrings.audioDeviceDefault else audioDeviceName)!!,
                            onValueChange = {},
                            label = {
                                Text(
                                    strings.settingsStrings.audioSettingsStrings.audioDevice,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    strings.contentDescriptionStrings.expandDropdownMenu,
                                    Modifier.clickable { audioDeviceExpanded = true },
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { audioDeviceExpanded = true },
                        )

                        DropdownMenu(
                            expanded = audioDeviceExpanded,
                            onDismissRequest = { audioDeviceExpanded = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            audioDeviceList.forEachIndexed { index, audioDevice ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            audioDevice,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    },
                                    onClick = {
                                        audioDeviceSelectedIndex = index
                                        if (defaultAudioDevice == audioDevice) {
                                            audioDeviceName = null
                                        }
                                        audioDeviceName = audioDevice
                                        audioDeviceExpanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = null,
                                    trailingIcon = null,
                                    enabled = true,
                                    colors = MenuDefaults.itemColors(),
                                    contentPadding = MenuDefaults.DropdownMenuItemContentPadding,
                                    interactionSource = audioDeviceInteractionSource,
                                )
                            }
                        }
                    }

                    // Length MS
                    TextField(
                        value = lengthMs.toString(),
                        onValueChange = { lengthMs = it.toIntOrNull() ?: lengthMs },
                        label = {
                            Text(
                                strings.settingsStrings.audioSettingsStrings.lengthMs,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        },
                    )

                    TextField(
                        value = delayMs.toString(),
                        onValueChange = { delayMs = it.toLongOrNull() ?: delayMs },
                        label = {
                            Text(
                                strings.settingsStrings.audioSettingsStrings.delayMs,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        },
                    )
                    // 翻译功能开关
                    RowOptionSwitch(
                        label =
                        strings.settingsStrings.audioSettingsStrings.translate,
                        isChecked = translate,
                        onCheckedChange = { translate = it },
                    )

                    SettingsTextField(
                        value = language ?: "",
                        label = strings.settingsStrings.audioSettingsStrings.language,
                        onValueChange = { language = it },
                    )

                    SettingsSectionTitle(strings.settingsStrings.openAiSettingsStrings.openAiSettingsTitle)
                    SettingsTextField(
                        value = openAiBaseUrl,
                        label = strings.settingsStrings.openAiSettingsStrings.openAiBaseUrl,
                        onValueChange = { openAiBaseUrl = it },
                    )
                    SettingsTextField(
                        value = openAiApiKey,
                        label = strings.settingsStrings.openAiSettingsStrings.openAiApiKey,
                        onValueChange = { openAiApiKey = it },
                    )
                    SettingsTextField(
                        value = systemPrompt,
                        label = strings.settingsStrings.openAiSettingsStrings.systemPrompt,
                        onValueChange = { systemPrompt = it },
                    )
                    // 最大令牌数
                    TextField(
                        value = maxTokens.toString(),
                        onValueChange = { maxTokens = it.toIntOrNull() ?: maxTokens },
                        label = {
                            Text(
                                strings.settingsStrings.openAiSettingsStrings.maxTokens,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        },
                    )
                    val temperatureRange = 0.0..1.0
                    val step = 0.1
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            strings.settingsStrings.openAiSettingsStrings.temperature,
                            modifier = Modifier.width(160.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        // 减号按钮
                        FilledTonalButton(
                            onClick = { if (temperature - step >= temperatureRange.start) temperature -= step },
                            enabled = temperature > temperatureRange.start,
                        ) {
                            Text(
                                "-",
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        // 数值展示
                        Text(
                            text = String.format("%.1f", temperature),
                            modifier = Modifier.width(64.dp).padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        // 加号按钮
                        FilledTonalButton(
                            onClick = { if (temperature + step <= temperatureRange.endInclusive) temperature += step },
                            enabled = temperature < temperatureRange.endInclusive,
                        ) {
                            Text(
                                "+",
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                    // 定义一个列表，包含下拉菜单的选项
                    val options = OpenAiChatModelName.entries.map { it.toString() }
                    // 使用CustomInputDropdown函数创建一个输入框
                    CustomInputDropdown(
                        value = preferredModel,
                        onValueChange = { preferredModel = it },
                        options = options,
                        label = strings.settingsStrings.openAiSettingsStrings.preferredModel
                    )
                    SettingsSectionTitle(strings.settingsStrings.workersAiSettingsStrings.workersAiSettingsTitle)
                    SettingsTextField(
                        value = accountId ?: "",
                        label = strings.settingsStrings.workersAiSettingsStrings.accountId,
                        onValueChange = { accountId = it },
                    )
                    SettingsTextField(
                        value = apiToken ?: "",
                        label = strings.settingsStrings.workersAiSettingsStrings.apiToken,
                        onValueChange = { apiToken = it },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = {
                            println(preferredModel)
                            Config.config.apply {
                                audioConfig.audioDeviceName = audioDeviceName
                                audioConfig.lengthMs = lengthMs
                                audioConfig.delayMs = delayMs
                                audioConfig.translate = translate
                                audioConfig.language = language
                                openAiConfig.openAiBaseUrl = openAiBaseUrl
                                openAiConfig.openAiApiKey = openAiApiKey.takeIf { it.isNotEmpty() }
                                openAiConfig.systemPrompt = systemPrompt.takeIf { it.isNotEmpty() }
                                openAiConfig.maxTokens = maxTokens
                                openAiConfig.temperature = temperature
                                openAiConfig.preferredModel = preferredModel
                                workersAiConfig.accountId = accountId
                                workersAiConfig.apiToken = apiToken
                            }
                            ConfigManager.saveConfig()
                        },
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(
                            strings.settingsStrings.saveSettings,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            ConfigManager.deleteConfig()
                            Config.config = ConfigManager.getConfig()
                            ConfigManager.saveConfig()
                            Singleton.showSettingsWindow = false
                        },
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(
                            strings.settingsStrings.resetSettings,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomInputDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(value) }

    Box {
        OutlinedTextField(
            value = tempValue,
            onValueChange = {
                tempValue = it
                onValueChange(it)  // 更新preferredModel的值
            },
            label = {
                Text(
                    label,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "expand dropdown menu",
                    Modifier.clickable { expanded = true },
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        tempValue = option
                        expanded = false
                        onValueChange(option)  // 更新preferredModel的值
                    }
                ) {
                    Text(
                        option,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(vertical = 10.dp),
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
fun SettingsTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier =
        Modifier.fillMaxWidth()
            .padding(vertical = 4.dp),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, color = MaterialTheme.colorScheme.onBackground) },
        modifier = modifier,
    )
}

@Composable
fun RowOptionSwitch(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
fun PreferableMaterialThemeDialog(onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            PreferableMaterialTheme {
                SettingsScaffold {
                    Box {
                        Column {
                            ThemePreferencesCategory()
                            ThemePreferenceItem()
                        }
                        themePrefs.showDialogIfNeed()
                    }
                }
            }
        }
    }
}

fun main() =
    application {
        Window(
            title = strings.appTitle,
            icon = painterResource("icon.png"),
            onCloseRequest = ::exitApplication,
        ) {
            App()
            if (Singleton.showAboutWindow) {
                About()
            }
            if (Singleton.showSettingsWindow) {
                Settings()
            }
            if (Singleton.showPreferableMaterialThemeDialog) {
                PreferableMaterialThemeDialog { Singleton.showPreferableMaterialThemeDialog = false }
            }
        }
    }
