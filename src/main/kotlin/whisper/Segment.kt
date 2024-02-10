package whisper

import androidx.compose.runtime.mutableStateListOf
import kotlin.math.abs

class Segment(var text: String?, var begin: Int, var end: Int) {

    companion object {
        var segments = mutableStateListOf<Segment>()
        fun add(newSegment: Segment) {
            if (segments.size > 0) {
                val existingSegment = segments.last()
                if (abs(existingSegment.begin - newSegment.begin) < 2
                    && (abs(existingSegment.end - newSegment.end) < 2 || existingSegment.end < newSegment.end)
                ) {
                    segments.remove(existingSegment)
                }
                segments.add(newSegment)
            }
        }
    }

    private var answer: String? = ""

    override fun toString(): String {
        return "Segment(text=$text, begin=$begin, end=$end, answer=$answer)"
    }
}
