import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import lombok.Data
import org.mongodb.kbson.ObjectId

@Data
class Config() : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    var openAiBaseUrl: String? = null

    var openAiApiKey: String? = null

    var promptTemplate: String? = null

    var maxTokens = 0

    var temperature = 0.0

    var enableCodeCompletion = false

    var preferredModel: String? = null
}