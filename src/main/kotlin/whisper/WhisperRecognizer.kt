package whisper

import Config
import io.github.givimad.whisperjni.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Paths
import javax.sound.sampled.*
import kotlin.math.max
import kotlin.math.min


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
        var isCapturing = MutableStateFlow(false)
        var samplesStep = Config.whisperConfig.stepMs * 1e-3 * Config.whisperConfig.sampleRate
        var samplesLen = Config.whisperConfig.lengthMs * 1e-3 * Config.whisperConfig.sampleRate
        var samplesKeep = Config.whisperConfig.keepMs * 1e-3 * Config.whisperConfig.sampleRate
    }

    init {
        setRecogniser()
    }

    private fun setRecogniser() {
        Config.whisperConfig.keepMs = min(Config.whisperConfig.keepMs, Config.whisperConfig.stepMs)
        Config.whisperConfig.lengthMs = max(Config.whisperConfig.lengthMs, Config.whisperConfig.stepMs)
        val loadOptions = WhisperJNI.LoadOptions().apply {
            logger = WhisperJNI.LibraryLogger { println(it) }
            whisperLib = Paths.get(Config.whisperConfig.whisperLib)
        }
        WhisperJNI.loadLibrary(loadOptions)
        WhisperJNI.setLibraryLogger(null)
        whisper = WhisperJNI()
        val whisperContextParams = WhisperContextParams()
        whisperContextParams.useGPU = Config.whisperConfig.useGPU
        whisperContext = whisper.init(Paths.get(Config.whisperConfig.model), whisperContextParams)
        whisperFullParams = WhisperFullParams(WhisperSamplingStrategy.GREEDY)
        whisperFullParams.language = Config.whisperConfig.language
        whisperFullParams.initialPrompt = Config.whisperConfig.initialPrompt
        whisperState = whisper.initState(whisperContext)
    }

    fun startRecognition(listener: RecognitionListener) = scope.launch {
        val format = AudioFormat(Config.whisperConfig.sampleRate, 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        line = AudioSystem.getLine(info) as TargetDataLine
        line.open(format)
        line.start()
        isCapturing.value = true
        stream = AudioInputStream(line)
        while (isCapturing.value) {
            val data = readSamples()
            val text = recognize(data)
            withContext(Dispatchers.Default) {
                listener.onResult(text)
            }
            delay(Config.whisperConfig.keepMs.toLong())
        }
    }

    @Throws(UnsupportedAudioFileException::class, IOException::class)
    private fun readSamples(): FloatArray {
        val captureBuffer =
            ByteBuffer.allocate((Config.whisperConfig.maxMs * 1e-3 * Config.whisperConfig.sampleRate).toInt())
        captureBuffer.order(ByteOrder.LITTLE_ENDIAN)
        var read: Int = -1
        if (stream.frameLength * Config.whisperConfig.sampleSizeInBits > 2 * samplesStep) {
            read = stream.read(captureBuffer.array())
        }
        if (stream.frameLength >= samplesStep) {
            read = stream.read(captureBuffer.array(), 0, samplesLen.toInt())
        } else {
            return FloatArray(0)
        }
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
        isCapturing.value = false
        line.stop()
    }

    fun closeRecognition() {
        isCapturing.value = false
        line.stop()
        line.close()
        job.cancel()
    }

    private fun recognize(data: FloatArray): String {
        var text = ""
        try {
            val result = whisper.fullWithState(whisperContext, whisperState, whisperFullParams, data, data.size)
            if (result != 0) {
                throw IOException("Transcription failed with code $result")
            }
            val numSegments = whisper.fullNSegmentsFromState(whisperState)
            if (numSegments > 0) {
                for (i in 0..<numSegments) {
                    text += whisper.fullGetSegmentTextFromState(whisperState, i)
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
