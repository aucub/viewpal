package asr

import config.Config
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import state.Segment
import java.io.File
import java.io.IOException

class WorkersAI {
    companion object {
        private val job = Job()
        private val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
            }
            expectSuccess = true
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, _ ->
                    val clientException =
                        exception as? ClientRequestException ?: return@handleResponseExceptionWithRequest
                    val exceptionResponse = clientException.response
                    if (exceptionResponse.status == HttpStatusCode.NotFound) {
                        val exceptionResponseText = exceptionResponse.bodyAsText()
                        throw IOException(exceptionResponseText)
                    }
                }
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }
        }
        private val scope = CoroutineScope(Dispatchers.IO + job)
        private val ASRUrl =
            "https://api.cloudflare.com/client/v4/accounts/${Config.config.workersAiConfig.accountId}/ai/run/@cf/openai/whisper"

        @OptIn(InternalAPI::class)
        fun transcribe(audioFile: File) = scope.async {
            val response: ASRResponse = client.post(ASRUrl) {
                header(HttpHeaders.Authorization, "Bearer ${Config.config.workersAiConfig.apiToken}")
                body = ByteArrayContent(audioFile.readBytes())
            }.body<ASRResponse>()
            Segment.addASRText(response.result.text)
        }

        private val summarizationUrl =
            "https://api.cloudflare.com/client/v4/accounts/${Config.config.workersAiConfig.accountId}/ai/run/@cf/facebook/bart-large-cnn"

        fun summary(inputText: String, index: Int) = scope.async {
            val response: SummaryResponse = client.post(summarizationUrl) {
                header(HttpHeaders.Authorization, "Bearer ${Config.config.workersAiConfig.apiToken}")
                contentType(ContentType.Application.Json)
                setBody(SummaryInput(inputText))
            }.body<SummaryResponse>()
            Segment.updateSummary(response.result.summary, index)
        }
    }
}

@Serializable
data class ASRResponse(
    val result: TextData,
    val success: Boolean,
    val errors: List<String>,
    val messages: List<String>
)

@Serializable
data class TextData(
    val text: String,
    @SerialName("word_count")
    val wordCount: Int,
    val words: List<Word>
)

@Serializable
data class Word(
    val word: String,
    val start: Double,
    val end: Double
)

@Serializable
data class SummaryInput(
    @SerialName("input_text")
    val inputText: String,

    @SerialName("max_length")
    val maxLength: Int = 1024
)

@Serializable
data class SummaryResponse(
    val result: SummaryData,
    val success: Boolean,
    val errors: List<String>,
    val messages: List<String>
)


@Serializable
data class SummaryData(
    @SerialName("summary")
    val summary: String
)