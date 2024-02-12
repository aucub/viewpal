package generator

import config.Config
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.memory.chat.TokenWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiTokenizer

class AnswerGenerator {
    companion object {
        private var tokenizer = OpenAiTokenizer(Config.config.preferredModel)
        private var model: ChatLanguageModel =
            OpenAiChatModel.builder().modelName(Config.config.preferredModel).maxTokens(Config.config.maxTokens)
                .tokenizer(tokenizer).temperature(Config.config.temperature).apiKey(Config.config.openAiApiKey)
                .baseUrl(Config.config.openAiBaseUrl).build()
        private var chatMemory = TokenWindowChatMemory.withMaxTokens(Config.config.maxTokens, tokenizer)

        fun generateAnswer(prompt: String): String {
            chatMemory.add(UserMessage.from(prompt))
            return model.generate(chatMemory.messages()).content().text()
        }
    }
}
