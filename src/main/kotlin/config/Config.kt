package config

import dev.langchain4j.model.openai.OpenAiChatModelName
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class Config {
    companion object {
        var config: Config = ConfigManager.getConfig()
    }

    var audioConfig: AudioConfig = AudioConfig()
    var openAiConfig: OpenAiConfig = OpenAiConfig()
    var workersAiConfig: WorkersAiConfig = WorkersAiConfig()
}

class AudioConfig : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()
    var audioDeviceName: String? = null
    var sampleRate: Float = 16000F
    var sampleSizeInBits: Int = 16
    var lengthMs = 15000
    var delayMs: Long = 10
    var translate: Boolean = false
    var language: String? = null
}

class OpenAiConfig : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()

    var openAiBaseUrl: String = "https://api.openai.com/v1/"

    var openAiApiKey: String? = null

    var maxTokens = 16 * 1000

    var temperature = 0.7

    var preferredModel: String = OpenAiChatModelName.GPT_3_5_TURBO.toString()

    var systemPrompt: String? = null
}


class WorkersAiConfig : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()
    var accountId: String? = null
    var apiToken: String? = null
}