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
        var samplesStep: Double = 0.0
        var samplesLen: Double = 0.0
        var data: FloatArray = FloatArray(samplesLen.toInt())
        var time = MutableStateFlow(0)
    }

    init {
        setRecogniser()
    }

    private fun setRecogniser() {
        Config.whisperConfig.lengthMs = max(Config.whisperConfig.lengthMs, Config.whisperConfig.stepMs)
        samplesStep = Config.whisperConfig.stepMs * 1e-3 * Config.whisperConfig.sampleRate
        samplesLen = Config.whisperConfig.lengthMs * 1e-3 * Config.whisperConfig.sampleRate
        data = FloatArray(samplesLen.toInt() / 2)
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

    fun startRecognition() = scope.launch {
        val format = AudioFormat(Config.whisperConfig.sampleRate, 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        line = AudioSystem.getLine(info) as TargetDataLine
        line.open(format)
        line.start()
        isCapturing.value = true
        stream = AudioInputStream(line)
        while (isCapturing.value) {
            readSamples()
            recognize(time.value, data)
            delay(Config.whisperConfig.delayMs.toLong())
        }
    }

    @Throws(UnsupportedAudioFileException::class, IOException::class)
    private fun readSamples() {
        val captureBuffer = ByteBuffer.allocate(samplesStep.toInt())
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
        val result = data.copyOfRange(samples.size, data.size)
        data = result + samples
        time.value += Config.whisperConfig.stepMs
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

    private fun recognize(time: Int, data: FloatArray) {
        var text: String
        if (time < Config.whisperConfig.lengthMs) return
        try {
            val result = whisper.fullWithState(whisperContext, whisperState, whisperFullParams, data, data.size)
            if (result != 0) {
                throw IOException("Transcription failed with code $result")
            }
            val numSegments = whisper.fullNSegmentsFromState(whisperState)
            if (numSegments > 0) {
                for (i in 0..<numSegments) {
                    val begin = whisper.fullGetSegmentTimestamp0FromState(whisperState, i).toInt()
                    val end = whisper.fullGetSegmentTimestamp1FromState(whisperState, i).toInt()
                    text = whisper.fullGetSegmentTextFromState(whisperState, i)
                    val segment = Segment(text, begin, end)
                    Segment.segments.add(segment)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
