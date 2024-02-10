import whisper.Segment

sealed class InteractionTarget {
    data class Element(val idx: Int) : InteractionTarget() {
        fun text(): String =
            Segment.segments[idx].text.toString()

        fun answer(): String =
            Segment.segments[idx].answer.toString()
    }
}