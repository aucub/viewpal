package generator

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel

class AnswerGenerator {
    private val model: ChatLanguageModel

    init {
        model = OpenAiChatModel.builder().modelName("").build()
    }

    fun generateAnswer(prompt: String): String {
        return model.generate(prompt)
    }
}
