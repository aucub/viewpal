package strings

internal data class Strings(
    val appTitle: String,
    val aboutTitle: String,
    val settingsTitle: String,
    val settingsStrings: SettingsStrings,
    val spotlightActionsStrings: SpotlightActionsStrings,
    val contentDescriptionStrings: ContentDescriptionStrings,
)

internal data class SettingsStrings(
    val saveSettings: String,
    val resetSettings: String,
    val openAiSettingsStrings: OpenAiSettingsStrings,
    val whisperSettingsStrings: WhisperSettingsStrings,
)

internal data class OpenAiSettingsStrings(
    val openAiSettingsTitle: String,
    val openAiBaseUrl: String,
    val openAiApiKey: String,
    val promptTemplate: String,
    val maxTokens: String,
    val temperature: String,
    val preferredModel: String,
    val topic: String,
)

internal data class WhisperSettingsStrings(
    val whisperSettingsTitle: String,
    val nThreads: String,
    val stepMs: String,
    val lengthMs: String,
    val keepMs: String,
    val delayMs: String,
    val translate: String,
    val detectLanguage: String,
    val language: String,
    val initialPrompt: String,
    val noContext: String,
    val useGPU: String,
    val whisperLib: String,
    val whisperLibPath: String,
    val selectWhisperLib: String,
    val model: String,
    val modelPath: String,
    val selectModel: String,
)

internal data class SpotlightActionsStrings(
    val first: String,
    val previous: String,
    val next: String,
    val last: String,
)

internal data class ContentDescriptionStrings(
    val preferableTheme: String,
    val info: String,
    val settings: String,
    val startNewSession: String,
    val pause: String,
    val capture: String,
    val expandDropdownMenu: String,
)
