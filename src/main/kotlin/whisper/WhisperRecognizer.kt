package whisper

import io.github.givimad.whisperjni.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Paths
import javax.sound.sampled.*


class WhisperRecognizer {

    private lateinit var line: TargetDataLine
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        lateinit var whisper: WhisperJNI
        lateinit var whisperContext: WhisperContext
        lateinit var whisperFullParams: WhisperFullParams
        lateinit var stream: AudioInputStream
        lateinit var whisperState: WhisperState
        var isRecording = MutableStateFlow(false)
    }


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
        whisper = WhisperJNI()
        val whisperContextParams = WhisperContextParams()
        whisperContextParams.useGPU = false
        whisperContext = whisper.init(Paths.get("/usr/share/whisper.cpp-model-medium/medium.bin"), whisperContextParams)
        whisperFullParams = WhisperFullParams(WhisperSamplingStrategy.GREEDY)
        whisperFullParams.language = "zh"
        whisperFullParams.initialPrompt = "以下是普通话的句子"
        whisperState = whisper.initState(whisperContext)
    }

    fun startRecognition(listener: RecognitionListener) = scope.launch {
        val format = AudioFormat(16000F, 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        line = AudioSystem.getLine(info) as TargetDataLine
        line.open(format)
        line.start()
        isRecording.value = true
        stream = AudioInputStream(line)
        while (isRecording.value) {
            val data = readSamples()
            val text = recognize(data)
            withContext(Dispatchers.Default) {
                listener.onResult(text)
            }
            delay(10L)
        }
    }

    @Throws(UnsupportedAudioFileException::class, IOException::class)
    private fun readSamples(): FloatArray {
        val captureBuffer = ByteBuffer.allocate(512 * 1024)
        captureBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val read = stream.read(captureBuffer.array())
        if (read == -1) {
            throw IOException("Empty capture")
        }
        val shortBuffer = captureBuffer.asShortBuffer()
        val samples = FloatArray(captureBuffer.capacity() / 2)
        var i = 0
        while (shortBuffer.hasRemaining()) {
            samples[i++] = java.lang.Float.max(
                -1f,
                java.lang.Float.min((shortBuffer.get().toFloat()) / Short.MAX_VALUE.toFloat(), 1f)
            )
        }
        return samples
    }

    fun stopRecognition() {
        isRecording.value = false
        line.stop()
    }

    fun closeRecognition() {
        isRecording.value = false
        line.stop()
        line.close()
        job.cancel()
    }

    private fun recognize(data: FloatArray): String {
        val text = ""
        try {
            val result = whisper.fullWithState(whisperContext, whisperState, whisperFullParams, data, data.size)
            if (result != 0) {
                throw IOException("Transcription failed with code $result")
            }
            val numSegments = whisper.fullNSegmentsFromState(whisperState)
            if (numSegments > 0) {
                for (i in 0..<numSegments) {
                    print(whisper.fullGetSegmentTextFromState(whisperState, i))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return text
    }


    interface RecognitionListener {
        fun onResult(text: String)
    }
}
