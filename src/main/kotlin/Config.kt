import dev.langchain4j.model.openai.OpenAiModelName
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class Config : RealmObject {

    companion object {
        lateinit var config: Config
    }

    @PrimaryKey
    var id: ObjectId = ObjectId()

    var openAiBaseUrl: String? = "https://api.openai.com/v1/"

    var openAiApiKey: String? = null

    var promptTemplate: String? = null

    var maxTokens = 0

    var temperature = 0.0

    var preferredModel: String? = OpenAiModelName.GPT_3_5_TURBO

    var jobName: String? = null

}