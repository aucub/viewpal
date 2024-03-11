package asr

import config.Config
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.Executors
import javax.sound.sampled.*

class Audio {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val scope =
            CoroutineScope(
                Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
            )
        private val sampleRate = Config.config.audioConfig.sampleRate
        private val sampleSizeInBits = Config.config.audioConfig.sampleSizeInBits
        private val lenMs = Config.config.audioConfig.lengthMs
        private var audioFormat = AudioFormat(sampleRate, sampleSizeInBits, 1, true, false)
        private lateinit var targetDataLine: TargetDataLine
        private var isCapturing = false
        fun getDeviceList(): List<String> = AudioSystem.getMixerInfo().map { it.name }
    }

    init {
        val dataLineInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
        targetDataLine = AudioSystem.getLine(dataLineInfo) as TargetDataLine
        targetDataLine.open(audioFormat, (sampleRate * lenMs / 1000).toInt())
        Config.config.audioConfig.audioDeviceName?.let { init(it) }
    }

    private fun init(deviceName: String): Boolean {
        val mixerInfoArray = AudioSystem.getMixerInfo()
        for (mixInfo in mixerInfoArray) {
            if (mixInfo.name.contains(deviceName)) {
                val mixer = AudioSystem.getMixer(mixInfo)
                val lineInfos = mixer.targetLineInfo
                for (lineInfo in lineInfos) {
                    if (AudioSystem.isLineSupported(lineInfo)) {
                        val info = lineInfo as DataLine.Info
                        targetDataLine = AudioSystem.getLine(info) as TargetDataLine
                        targetDataLine.open(audioFormat, (sampleRate * lenMs / 1000).toInt())
                        return true
                    }
                }
            }
        }
        return false
    }

    fun resume(): Boolean {
        if (isCapturing) {
            logger.warn { "Already capturing!" }
            return false
        }
        isCapturing = true
        targetDataLine.start()
        return true
    }

    private fun createTempWavFile(audioBytes: ByteArray): File {
        val sampleFormat = AudioFormat(sampleRate, sampleSizeInBits, 1, true, false)
        val bais = ByteArrayInputStream(audioBytes)
        val audioInputStream = AudioInputStream(bais, sampleFormat, audioBytes.size.toLong())
        val tempFile = File.createTempFile("temp", ".wav")
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, tempFile)
        return tempFile
    }

    fun start() = scope.launch {
        targetDataLine.start()
        isCapturing = true
        while (isCapturing) {
            val audioBytes = ByteArray((sampleRate * lenMs / 1000).toInt())
            val bytesRead = readSamples(lenMs, audioBytes)
            if (bytesRead > 0) {
                val tempFile = createTempWavFile(audioBytes)
                WorkersAI.transcribe(tempFile)
            }
            Thread.sleep(Config.config.audioConfig.delayMs)
        }
    }

    private fun readSamples(ms: Int, audioBytes: ByteArray): Int {
        if (!isCapturing) {
            logger.warn { "Not capturing!" }
            return 0
        }
        val msLocal = if (ms <= 0) lenMs else ms
        val nSamples = (sampleRate * msLocal / 1000).toInt()
        if (nSamples <= 0) return 0
        return targetDataLine.read(audioBytes, 0, nSamples)
    }

    fun clear(): Boolean {
        if (!isCapturing) {
            logger.warn { "Not capturing!" }
            return false
        }
        targetDataLine.close()
        return true
    }

    fun pause(): Boolean {
        if (!isCapturing) {
            logger.warn { "Already paused!" }
            return false
        }
        isCapturing = false
        targetDataLine.stop()
        return true
    }
}