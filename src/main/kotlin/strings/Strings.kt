package strings

internal data class Strings(
    val appTitle: String,
    val aboutTitle: String,
    val settingsTitle: String,
    val settingsStrings: SettingsStrings,
    val contentDescriptionStrings: ContentDescriptionStrings,
)

internal data class SettingsStrings(
    val saveSettings: String,
    val resetSettings: String,
    val audioSettingsStrings: AudioSettingsStrings,
    val openAiSettingsStrings: OpenAiSettingsStrings,
    val workersAiSettingsStrings: WorkersAiSettingsStrings,
)

internal data class AudioSettingsStrings(
    val audioSettingsTitle: String,
    val audioDevice: String,
    val audioDeviceDefault: String,
    val lengthMs: String,
    val delayMs: String,
    val translate: String,
    val language: String
)

internal data class OpenAiSettingsStrings(
    val openAiSettingsTitle: String,
    val openAiBaseUrl: String,
    val openAiApiKey: String,
    val systemPrompt: String,
    val maxTokens: String,
    val temperature: String,
    val preferredModel: String,
)

internal data class WorkersAiSettingsStrings(
    val workersAiSettingsTitle: String,
    val accountId: String,
    val apiToken: String
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
