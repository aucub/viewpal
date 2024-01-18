package generator

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel

class AnswerGenerator {
    private var model: ChatLanguageModel = OpenAiChatModel.builder().modelName("").build()

    fun generateAnswer(prompt: String): String {
        return model.generate(prompt)
    }
}
