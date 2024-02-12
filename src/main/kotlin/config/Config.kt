package config

import dev.langchain4j.model.openai.OpenAiChatModelName
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class Config {
    companion object {
        var config: Config = ConfigManager.getConfig()
    }

    fun deepCopy(): Config {
        var copiedConfig = Config()
        copiedConfig.openAiConfig = this.openAiConfig.deepCopy()
        copiedConfig.whisperConfig = this.whisperConfig.deepCopy()
        return copiedConfig
    }

    var openAiConfig: OpenAiConfig = OpenAiConfig()
    var whisperConfig: WhisperConfig = WhisperConfig()
}

class OpenAiConfig : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()

    var openAiBaseUrl: String = "https://api.openai.com/v1/"

    var openAiApiKey: String? = null

    var promptTemplate: String? = null

    var maxTokens = 16 * 1000

    var temperature = 0.7

    var preferredModel: String = OpenAiChatModelName.GPT_3_5_TURBO.toString()

    var topic: String? = null
}

class WhisperConfig : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()

    var sampleRate: Float = 16000F

    var sampleSizeInBits: Int = 16

    var nThreads: Int = 0

    var stepMs: Int = 3000

    var lengthMs = 10000

    var keepMs = 200

    var delayMs: Long = 1

    var translate: Boolean = false

    var detectLanguage: Boolean = false

    var noTimestamps: Boolean = false

    var language: String = "zh"

    var printSpecial: Boolean = false

    var audioCtx: Int = 0

    var speedUp: Boolean = false

    var noFallback: Boolean = false

    var vadThold = 0.6f

    var freqThold = 100.0f

    var initialPrompt: String? = "以下是普通话的句子"

    var noContext: Boolean = true

    var useGPU: Boolean = true

    var whisperLib: String = "/usr/lib/libwhisper.so"

    var model: String = "/usr/share/whisper.cpp-model-base/base.bin"
}

fun OpenAiConfig.deepCopy(): OpenAiConfig {
    var copy = OpenAiConfig()
    copy.id = this.id
    copy.openAiBaseUrl = this.openAiBaseUrl
    copy.openAiApiKey = this.openAiApiKey
    copy.promptTemplate = this.promptTemplate
    copy.maxTokens = this.maxTokens
    copy.temperature = this.temperature
    copy.preferredModel = this.preferredModel
    copy.topic = this.topic
    return copy
}

fun WhisperConfig.deepCopy(): WhisperConfig {
    var copy = WhisperConfig()
    copy.id = this.id
    copy.sampleRate = this.sampleRate
    copy.sampleSizeInBits = this.sampleSizeInBits
    copy.nThreads = this.nThreads
    copy.stepMs = this.stepMs
    copy.lengthMs = this.lengthMs
    copy.keepMs = this.keepMs
    copy.delayMs = this.delayMs
    copy.translate = this.translate
    copy.detectLanguage = this.detectLanguage
    copy.noTimestamps = this.noTimestamps
    copy.language = this.language
    copy.printSpecial = this.printSpecial
    copy.audioCtx = this.audioCtx
    copy.speedUp = this.speedUp
    copy.noFallback = this.noFallback
    copy.vadThold = this.vadThold
    copy.freqThold = this.freqThold
    copy.initialPrompt = this.initialPrompt
    copy.noContext = this.noContext
    copy.useGPU = this.useGPU
    copy.whisperLib = this.whisperLib
    copy.model = this.model
    return copy
}
