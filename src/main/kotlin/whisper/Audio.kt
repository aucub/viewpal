package whisper

import config.Config
import java.util.concurrent.locks.ReentrantLock
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.withLock

class Audio {
    private var mLenMs = Config.whisperConfig.lengthMs
    private var mSampleRate = Config.whisperConfig.sampleRate
    private var audioFormat: AudioFormat = AudioFormat(
        mSampleRate,
        Config.whisperConfig.sampleSizeInBits,
        1,
        true,
        false
    )
    private var targetDataLine: TargetDataLine
    private var isRunning: Boolean = false
    private val audioBufferLock = ReentrantLock()
    private var audioBuffer = FloatArray(0)
    private var audioPosition = 0

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
        if (!isRunning) {
            isRunning = true
            targetDataLine.start()
            return true
        } else {
            println("Already running!")
            return false
        }
    }

    fun get(ms: Int, result: MutableList<Float>) {
        if (!isRunning) {
            println("Not running!")
            return
        }

        var msLocal = ms
        if (msLocal <= 0) {
            msLocal = mLenMs
        }

        val nSamples = (mSampleRate * msLocal / 1000).toInt()

        val requiredSamples = nSamples.coerceAtMost(audioBuffer.size)
        result.clear()

        if (requiredSamples > 0) {
            audioBufferLock.withLock {
                val s0 = if (audioPosition - requiredSamples < 0) {
                    audioBuffer.size + audioPosition - requiredSamples
                } else {
                    audioPosition - requiredSamples
                }

                if (s0 + requiredSamples > audioBuffer.size) {
                    val n0 = audioBuffer.size - s0
                    (s0 until audioBuffer.size).forEach { result.add(audioBuffer[it]) }
                    (0 until requiredSamples - n0).forEach { result.add(audioBuffer[it]) }
                } else {
                    (s0 until s0 + requiredSamples).forEach { result.add(audioBuffer[it]) }
                }
            }
        }
    }

    fun clear(): Boolean {
        if (!isRunning) {
            println("Not running!")
            return false
        }

        audioBufferLock.withLock {
            audioBuffer = FloatArray(0)
            audioPosition = 0
        }

        return true
    }

    fun pause(): Boolean {
        if (!isRunning) {
            println("Already paused!")
            return false
        }
        isRunning = false
        targetDataLine.stop()
        return true
    }
}
