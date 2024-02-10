package whisper

class Segment(var text: String?, var begin: Int, var end: Int) {

    companion object {
        var segments = ArrayList<Segment>()
    }

    var answer: String? = ""

}
