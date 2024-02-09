package config

import dev.langchain4j.model.openai.OpenAiChatModelName
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class Config : RealmObject {

    companion object {
        var config: Config = Config()
        var whisperConfig: WhisperConfig = WhisperConfig()
    }

    @PrimaryKey
    var id: ObjectId = ObjectId()

    var openAiBaseUrl: String = "https://api.openai.com/v1/"

    var openAiApiKey: String? = null

    var promptTemplate: String? = null

    var maxTokens = 16 * 1000

    var temperature = 0.7

    var preferredModel: String = OpenAiChatModelName.GPT_3_5_TURBO.toString()

    var topic: String? = null

    class WhisperConfig : RealmObject {

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

}