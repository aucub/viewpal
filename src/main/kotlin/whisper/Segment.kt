package whisper

class Segment(private var text: String?, private var begin: Int, private var end: Int) {

    companion object {
        var segments = ArrayList<Segment>()
    }

    private var answer: String? = ""

}
