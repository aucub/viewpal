package whisper

import io.github.givimad.whisperjni.WhisperContext
import io.github.givimad.whisperjni.WhisperFullParams
import io.github.givimad.whisperjni.WhisperJNI
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.nio.file.Paths
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

class WhisperRecognizer {

    private lateinit var line: TargetDataLine
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        var whisper = WhisperJNI()
        val whisperContext: WhisperContext by lazy { whisper.init(Paths.get("/usr/share/whisper.cpp-model-tiny/tiny.bin")) }
    }

    private val isRecording = MutableStateFlow(false)

    init {
        setRecogniser()
    }

    private fun setRecogniser() {
        val loadOptions = WhisperJNI.LoadOptions().apply {
            logger = WhisperJNI.LibraryLogger { println(it) }
            whisperLib = Paths.get("/usr/lib/libwhisper.so")
        }
        WhisperJNI.loadLibrary(loadOptions)
        WhisperJNI.setLibraryLogger(null)
    }

    fun startRecognition(listener: RecognitionListener) = scope.launch {
        val format = AudioFormat(16000F, 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        line = AudioSystem.getLine(info) as TargetDataLine
        line.open(format)
        line.start()
        isRecording.value = true

        while (isRecording.first()) {
            val data = getLatestData()
            val text = recognize(data)
            withContext(Dispatchers.Main) {
                listener.onResult(text)
            }
        }
    }

    private fun getLatestData(): FloatArray {
        val buffer = ByteArray(1024)
        val bytesRead = line.read(buffer, 0, buffer.size)

        return if (bytesRead > 0) {
            ByteBuffer.wrap(buffer).asShortBuffer().toFloatArray()
        } else FloatArray(0)
    }

    private fun ShortBuffer.toFloatArray(): FloatArray {
        val floatBuffer = FloatBuffer.allocate(capacity())
        while (hasRemaining()) {
            floatBuffer.put(get() / 32768.0f)
        }
        return floatBuffer.array()
    }

    fun stopRecognition() {
        isRecording.value = false
        line.stop()
        line.close()
        job.cancel()
    }

    private fun recognize(data: FloatArray): String {
        var text = ""
        try {
            val params = WhisperFullParams()
            val result = whisper.full(whisperContext, params, data, data.size)
            if (result != 0) {
                throw IOException("Transcription failed with code $result")
            }
            text = whisper.fullGetSegmentText(whisperContext, 0)
            val numSegments = whisper.fullNSegments(whisperContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return text
    }

    interface RecognitionListener {
        fun onResult(text: String)
    }
}
