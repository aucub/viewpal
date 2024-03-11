package state

import androidx.compose.runtime.mutableStateListOf
import asr.WorkersAI
import generator.PromptGenerator
import generator.TextGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Segment {
    companion object {
        private val scope = CoroutineScope(Dispatchers.Default)
        var segments = mutableStateListOf<Segment>()
        var asrTexts = mutableStateListOf<String>()
        private var summary: String = ""
        private var index: Int = 0
        private val segmentsLock = ReentrantLock()
        private val asrTextsLock = ReentrantLock()
        private val summaryLock = ReentrantLock()
        fun clear() {
            asrTextsLock.withLock {
                asrTexts.clear()
            }
            segmentsLock.withLock {
                segments.clear()
            }
            summaryLock.withLock {
                summary = ""
                index = 0
            }
        }

        fun updateSummary(newSummary: String, newIndex: Int) {
            summaryLock.withLock {
                summary = newSummary
                index = newIndex
            }
        }

        fun addASRText(text: String) =
            scope.launch {
                var asrTextIndex: Int
                asrTextsLock.withLock {
                    asrTexts.add(text)
                    asrTextIndex = asrTexts.size - 1
                }
                if (containsQuestion(text)) {
                    val dialogue = StringBuffer()
                    dialogue.append(summary)
                    for (i in index..asrTextIndex) {
                        dialogue.append(asrTexts[i])
                    }
                    if (dialogue.length > 1024) {
                        WorkersAI.summary(dialogue.toString(), asrTextIndex)
                    }
                    val segment = Segment()
                    val questionPrompt = PromptGenerator.extractQuestion(dialogue.toString())
                    segment.prompt = TextGenerator.generateAnswer(questionPrompt)
                    segment.answer = TextGenerator.generateAnswer(segment.prompt!!)
                    segmentsLock.withLock {
                        segments.add(segment)
                    }
                }
            }

        private fun containsQuestion(text: String): Boolean {
            val questionWords = listOf(
                "什么",
                "怎么",
                "为什么",
                "哪里",
                "是不是",
                "吗",
                "么",
                "嘛",
                "呢",
                "怎样",
                "如何",
                "何时",
                "何处",
                "何人",
                "几时",
                "几处",
                "几人",
                "哪",
                "几",
                "多少",
                "\\？",
                "what",
                "where",
                "when",
                "why",
                "how",
                "who",
                "which",
                "whom",
                "whose",
                "\\?"
            )
            val regex = Regex("\\b(" + questionWords.joinToString("|") + ")\\b", RegexOption.IGNORE_CASE)
            return regex.containsMatchIn(text)
        }
    }


    var prompt: String? = ""

    var answer: String? = ""
}