package whisper

import androidx.compose.runtime.mutableStateListOf
import generator.AnswerGenerator
import generator.PromptGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.abs

class Segment(var text: String?, var begin: Int, var end: Int) {

    companion object {
        private val scope = CoroutineScope(Dispatchers.Default)
        var segments = mutableStateListOf<Segment>()
        private val segmentsLock = ReentrantLock()
        fun init() {
            segmentsLock.withLock {
                if (Segment.segments.size == 0) {
                    Segment.segments.add(Segment("", 0, 0))
                }
            }
        }

        fun clear() {
            segmentsLock.withLock {
                segments.clear()
            }
        }

        fun add(newSegment: Segment) = scope.launch {
            var index: Int
            if (segments.size > 0) {
                val existingSegment = segments.last()
                if (abs(existingSegment.begin - newSegment.begin) < 2
                    && (abs(existingSegment.end - newSegment.end) < 2 || existingSegment.end < newSegment.end)
                ) {
                    segmentsLock.withLock {
                        segments.remove(existingSegment)
                        segments.add(newSegment)
                        index = segments.size - 1
                    }
                }
            }
            segmentsLock.withLock {
                segments.add(newSegment)
                index = segments.size - 1
            }
            var dialogue = StringBuffer()
            for (segment in segments) {
                dialogue.append(segment.text)
            }
            newSegment.prompt = PromptGenerator.extractQuestion(dialogue.toString())
            newSegment.answer = AnswerGenerator.generateAnswer(newSegment.prompt!!)
            segmentsLock.withLock {
                segments[index] = newSegment
            }
        }
    }

    var prompt: String? = ""

    var answer: String? = ""
}
