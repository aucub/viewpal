package generator

import com.knuddels.jtokkit.Encodings
import config.Config
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.memory.chat.TokenWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiChatModelName
import dev.langchain4j.model.openai.OpenAiTokenizer

class TextGenerator {
    companion object {
        private var tokenizer: OpenAiTokenizer = if (Encodings.newLazyEncodingRegistry()
                .getEncodingForModel(Config.config.openAiConfig.preferredModel).isEmpty
        ) {
            OpenAiTokenizer(OpenAiChatModelName.GPT_3_5_TURBO.toString())
        } else {
            OpenAiTokenizer(Config.config.openAiConfig.preferredModel)
        }
        private var model: ChatLanguageModel =
            OpenAiChatModel.builder().modelName(Config.config.openAiConfig.preferredModel)
                .maxTokens(Config.config.openAiConfig.maxTokens)
                .tokenizer(tokenizer).temperature(Config.config.openAiConfig.temperature)
                .apiKey(Config.config.openAiConfig.openAiApiKey)
                .baseUrl(Config.config.openAiConfig.openAiBaseUrl).build()
        private var chatMemory = TokenWindowChatMemory.withMaxTokens(Config.config.openAiConfig.maxTokens, tokenizer)

        init {
            val systemPrompt = Config.config.openAiConfig.systemPrompt
            if (systemPrompt != null) {
                chatMemory.add(SystemMessage(systemPrompt))
            }
        }

        fun generateAnswer(prompt: String): String {
            chatMemory.add(UserMessage.from(prompt))
            return model.generate(chatMemory.messages()).content().text()
        }
    }
}
