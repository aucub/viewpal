package whisper

class Segment {

    companion object {
        var segments: List<Segment> = emptyList()
    }

    private var text: String? = ""

    private var answer: String? = ""

    private val begin = 0

    private val end = 0
}
