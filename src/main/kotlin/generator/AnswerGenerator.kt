package generator

import config.Config
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.memory.chat.TokenWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiTokenizer

class AnswerGenerator {
    companion object {
        private var tokenizer = OpenAiTokenizer(Config.config.openAiConfig.preferredModel)
        private var model: ChatLanguageModel =
            OpenAiChatModel.builder().modelName(Config.config.openAiConfig.preferredModel)
                .maxTokens(Config.config.openAiConfig.maxTokens)
                .tokenizer(tokenizer).temperature(Config.config.openAiConfig.temperature)
                .apiKey(Config.config.openAiConfig.openAiApiKey)
                .baseUrl(Config.config.openAiConfig.openAiBaseUrl).build()
        private var chatMemory = TokenWindowChatMemory.withMaxTokens(Config.config.openAiConfig.maxTokens, tokenizer)

        fun generateAnswer(prompt: String): String {
            chatMemory.add(UserMessage.from(prompt))
            return model.generate(chatMemory.messages()).content().text()
        }
    }
}
