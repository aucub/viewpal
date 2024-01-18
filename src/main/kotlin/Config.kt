import dev.langchain4j.model.openai.OpenAiModelName
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class Config : RealmObject {

    companion object {
        lateinit var config: Config
        lateinit var whisperConfig: WhisperConfig
    }

    @PrimaryKey
    var id: ObjectId = ObjectId()

    var openAiBaseUrl: String = "https://api.openai.com/v1/"

    var openAiApiKey: String? = null

    var promptTemplate: String? = null

    var maxTokens = 16 * 1000

    var temperature = 0.7

    var preferredModel: String = OpenAiModelName.GPT_3_5_TURBO

    var jobName: String? = null

    class WhisperConfig : RealmObject {

        val sampleRate: Float = 16000F

        var sampleSizeInBits: Int = 16

        var nThreads: Int = 0

        var stepMs: Int = 3000

        var lengthMs = 10000

        var keepMs = 200

        var maxMs = 30000

        var translate: Boolean = false

        var detectLanguage: Boolean = false

        var language: String = "zh"

        var initialPrompt: String? = "以下是普通话的句子"

        var noContext: Boolean = false

        var useGPU: Boolean = false

        var whisperLib: String = "/usr/lib/libwhisper.so"

        var model: String = "/usr/share/whisper.cpp-model-medium/medium.bin"
    }

}