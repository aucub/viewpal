package whisper

import config.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.withLock

class Audio {
    companion object {
        private val scope =
            CoroutineScope(
                Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
            )
        private var mLenMs = Config.config.whisperConfig.lengthMs
        private var mSampleRate = Config.config.whisperConfig.sampleRate
        private var audioFormat: AudioFormat =
            AudioFormat(
                mSampleRate,
                Config.config.whisperConfig.sampleSizeInBits,
                1,
                true,
                false,
            )
        private lateinit var targetDataLine: TargetDataLine
        private var isCapturing: Boolean = false
        private val audioQueue: Queue<FloatArray> = LinkedList()
        private val audioQueueLock = ReentrantLock()
    }

    init {
        val dataLineInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
        targetDataLine = AudioSystem.getLine(dataLineInfo) as TargetDataLine
        targetDataLine.open(audioFormat, (mSampleRate * mLenMs / 1000).toInt())
    }

    fun getDeviceList(): List<String> {
        val mixerInfoArray = AudioSystem.getMixerInfo()
        val deviceList = mutableListOf<String>()
        for (mixInfo in mixerInfoArray) {
            deviceList.add(mixInfo.name)
        }
        return deviceList
    }

    fun init(deviceName: String): Boolean {
        val mixerInfoArray = AudioSystem.getMixerInfo()
        for (mixInfo in mixerInfoArray) {
            if (mixInfo.name.contains(deviceName)) {
                val mixer = AudioSystem.getMixer(mixInfo)
                val lineInfo = mixer.targetLineInfo
                if (lineInfo.isNotEmpty() && AudioSystem.isLineSupported(lineInfo.first())) {
                    val info = lineInfo.first() as DataLine.Info
                    targetDataLine = AudioSystem.getLine(info) as TargetDataLine
                    targetDataLine.open(audioFormat, (mSampleRate * mLenMs / 1000).toInt())
                    return true
                }
            }
        }
        return false
    }

    fun resume(): Boolean {
        if (!isCapturing) {
            isCapturing = true
            targetDataLine.start()
            return true
        } else {
            println("Already capturing!")
            return false
        }
    }

    fun start() =
        scope.launch {
            targetDataLine.start()
            isCapturing = true
            while (isCapturing) {
                val samples = readSamples(mLenMs)
                if (samples != null) {
                    audioQueueLock.withLock {
                        audioQueue.add(samples)
                    }
                }
                Thread.sleep(Config.config.whisperConfig.delayMs)
            }
        }

    fun get(): FloatArray? {
        audioQueueLock.withLock {
            try {
                return audioQueue.remove()
            } catch (e: NoSuchElementException) {
                return null
            }
        }
    }

    private fun readSamples(ms: Int): FloatArray? {
        if (!isCapturing) {
            println("Not capturing!")
            return null
        }
        var msLocal = ms
        if (msLocal <= 0) {
            msLocal = mLenMs
        }
        val nSamples = (mSampleRate * msLocal / 1000).toInt()
        if (nSamples > 0) {
            val captureBuffer: ByteBuffer = ByteBuffer.allocate(nSamples)
            captureBuffer.order(ByteOrder.LITTLE_ENDIAN)
            targetDataLine.read(captureBuffer.array(), 0, nSamples)
            val shortBuffer = captureBuffer.asShortBuffer()
            val samples = FloatArray(captureBuffer.capacity() / 2)
            var i = 0
            while (shortBuffer.hasRemaining()) {
                samples[i++] =
                    java.lang.Float.max(
                        -1f,
                        java.lang.Float.min((shortBuffer.get().toFloat()) / Short.MAX_VALUE.toFloat(), 1f),
                    )
            }
            return samples
        } else {
            return null
        }
    }

    fun clear(): Boolean {
        if (!isCapturing) {
            println("Not capturing!")
            return false
        }
        targetDataLine.close()
        audioQueueLock.withLock {
            audioQueue.clear()
        }
        return true
    }

    fun pause(): Boolean {
        if (!isCapturing) {
            println("Already paused!")
            return false
        }
        isCapturing = false
        targetDataLine.stop()
        return true
    }
}
