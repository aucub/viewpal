package strings

import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = Locales.EN, default = true)
internal val EnStrings =
    Strings(
        appTitle = "viewpal",
        aboutTitle = "AboutLibraries",
        settingsTitle = "Settings",
        settingsStrings =
        SettingsStrings(
            saveSettings = "Save Settings",
            resetSettings = "Reset Settings",
            audioSettingsStrings =
            AudioSettingsStrings(
                audioSettingsTitle = "Audio Config",
                audioDevice = "Audio Device",
                audioDeviceDefault = "Default Device",
                translate = "Translate",
                language = "Language",
                lengthMs = "Length (ms)",
                delayMs = "Delay (ms)"
            ),
            openAiSettingsStrings =
            OpenAiSettingsStrings(
                openAiSettingsTitle = "OpenAI Config",
                openAiBaseUrl = "OpenAI Base URL",
                openAiApiKey = "OpenAI API Key",
                systemPrompt = "System Prompt",
                maxTokens = "Max Tokens",
                temperature = "Temperature",
                preferredModel = "Preferred Model",
            ),
            workersAiSettingsStrings = WorkersAiSettingsStrings(
                workersAiSettingsTitle = "WorkersAI Config",
                accountId = "ACCOUNT ID",
                apiToken = "API token"
            )
        ),
        contentDescriptionStrings =
        ContentDescriptionStrings(
            preferableTheme = "PreferableTheme",
            info = "Info",
            settings = "Settings",
            startNewSession = "New",
            pause = "Pause",
            capture = "Capture",
            expandDropdownMenu = "Expand dropdown menu",
        ),
    )
