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
            openAiSettingsStrings =
            OpenAiSettingsStrings(
                openAiSettingsTitle = "OpenAI Config",
                openAiBaseUrl = "OpenAI Base URL",
                openAiApiKey = "OpenAI API Key",
                promptTemplate = "Prompt Template",
                maxTokens = "Max Tokens",
                temperature = "Temperature",
                preferredModel = "Preferred Model",
                topic = "Topic",
            ),
            whisperSettingsStrings =
            WhisperSettingsStrings(
                whisperSettingsTitle = "Whisper Config",
                nThreads = "Number of Threads",
                stepMs = "Step (ms)",
                lengthMs = "Length (ms)",
                keepMs = "Keep (ms)",
                delayMs = "Delay (ms)",
                translate = "Translate",
                detectLanguage = "DetectLanguage",
                language = "Language",
                initialPrompt = "Initial Prompt",
                noContext = "Disable Context",
                useGPU = "Use GPU",
                whisperLib = "Whisper Library",
                whisperLibPath = "Whisper Library Path",
                selectWhisperLib = "Select Whisper Library",
                model = "Model Binary",
                modelPath = "Model Binary Path",
                selectModel = "Select Model Binary",
            ),
        ),
        spotlightActionsStrings =
        SpotlightActionsStrings(
            first = "First",
            previous = "Prev",
            next = "Next",
            last = "Last",
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
