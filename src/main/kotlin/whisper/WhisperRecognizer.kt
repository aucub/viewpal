package whisper

import config.Config
import io.github.givimad.whisperjni.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class WhisperRecognizer {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        val nThreads = minOf(Config.whisperConfig.nThreads, Runtime.getRuntime().availableProcessors())
        lateinit var whisper: WhisperJNI
        lateinit var whisperContext: WhisperContext
        var whisperFullParams: WhisperFullParams = WhisperFullParams(WhisperSamplingStrategy.GREEDY)
        var isRunning = false
        var nSamplesStep: Double = 0.0
        var nSamplesLen: Double = 0.0
        var nSamplesKeep: Double = 0.0
        var nSamples30s: Double = 0.0
        var samplesOld = mutableListOf<Float>()
        var samplesNew = mutableListOf<Float>()
        var samples = mutableListOf<Float>()
        var useVad = false
        lateinit var audio: Audio
        var nNewLine = 1
        var promptTokens: HashSet<String> = HashSet()
    }

    init {
        Config.whisperConfig.keepMs = min(Config.whisperConfig.keepMs, Config.whisperConfig.stepMs)
        Config.whisperConfig.lengthMs = max(Config.whisperConfig.lengthMs, Config.whisperConfig.stepMs)
        nSamplesStep = Config.whisperConfig.stepMs * 1e-3 * Config.whisperConfig.sampleRate
        nSamplesLen = Config.whisperConfig.lengthMs * 1e-3 * Config.whisperConfig.sampleRate
        nSamplesKeep = Config.whisperConfig.keepMs * 1e-3 * Config.whisperConfig.sampleRate
        nSamples30s = 30000.0 * 1e-3 * Config.whisperConfig.sampleRate
        useVad = nSamplesStep <= 0
        nNewLine = if (!useVad) max(1, Config.whisperConfig.lengthMs / Config.whisperConfig.stepMs - 1) else 1
        whisperFullParams.nThreads = nThreads
        whisperFullParams.noTimestamps = !useVad
        whisperFullParams.noContext = Config.whisperConfig.noContext || useVad
        whisperFullParams.printProgress = false
        whisperFullParams.printSpecial = Config.whisperConfig.printSpecial
        whisperFullParams.printRealtime = false
        whisperFullParams.printTimestamps = !Config.whisperConfig.noTimestamps
        whisperFullParams.singleSegment = !useVad
        whisperFullParams.translate = Config.whisperConfig.translate
        whisperFullParams.audioCtx = Config.whisperConfig.audioCtx
        whisperFullParams.speedUp = Config.whisperConfig.speedUp
        whisperFullParams.language = Config.whisperConfig.language
        whisperFullParams.initialPrompt = Config.whisperConfig.initialPrompt
        whisperFullParams.temperatureInc =
            if (Config.whisperConfig.noFallback) 0.0f else whisperFullParams.temperatureInc
        audio = Audio()
        audio.resume()
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

        if (!whisper.isMultilingual(whisperContext)) {
            if (Config.whisperConfig.translate) {
                Config.whisperConfig.translate = false
                System.err.println("警告：模型不是多语言的，忽略语言和翻译选项")
            }
        }
    }

    fun startRecognition() = scope.launch {
        isRunning = true
        var nIter = 0
        var tLast = System.nanoTime()
        while (isRunning) {
            if (!isRunning) {
                break
            }
            if (!useVad) {
                while (true) {
                    audio.get(Config.whisperConfig.stepMs, samplesNew)
                    if (samplesNew.size > 2 * nSamplesStep) {
                        System.err.println("警告：无法足够快地处理音频，丢弃音频......")
                        audio.clear()
                        continue
                    }
                    if (samplesNew.size >= nSamplesStep) {
                        audio.clear()
                        break
                    }
                    Thread.sleep(Config.whisperConfig.delayMs)
                }
                val nSamplesNew = samplesNew.size
                val nSamplesTake = min(samplesOld.size, max(0, (nSamplesKeep + nSamplesLen - nSamplesNew).toInt()))
                val samples = FloatArray(nSamplesNew + samplesOld.size)
                for (i in 0 until nSamplesTake) {
                    samples[i] = samplesOld[samplesOld.size - nSamplesTake + i]
                }
                for (i in 0 until nSamplesNew) {
                    samples[nSamplesTake + i] = samplesNew[i]
                }
                samplesOld = samples.toMutableList()
            } else {
                val tNow = System.nanoTime()
                val tDiff = TimeUnit.NANOSECONDS.toMillis(tNow - tLast)
                if (tDiff < 2000) {
                    Thread.sleep(100)
                    continue
                }
                audio.get(2000, samplesNew)
                if (vadSimple(
                        samplesNew.toFloatArray(), Config.whisperConfig.sampleRate,
                        1000, Config.whisperConfig.vadThold, Config.whisperConfig.freqThold, false
                    )
                ) {
                    audio.get(Config.whisperConfig.lengthMs, samples.toMutableList())
                } else {
                    Thread.sleep(100)
                    continue
                }
                tLast = tNow
            }
            if (whisper.full(whisperContext, whisperFullParams, samples.toFloatArray(), samples.size) != 0) {
                println("音频处理失败")
                return@launch
            }
            val nSegments = whisper.fullNSegments(whisperContext)
            if (nSegments > 0) {
                for (i in 0 until nSegments) {
                    val text = whisper.fullGetSegmentText(whisperContext, i)
                    if (whisperFullParams.noTimestamps) {
                    } else {
                        val t0 = whisper.fullGetSegmentTimestamp0(whisperContext, i)
                        val t1 = whisper.fullGetSegmentTimestamp1(whisperContext, i)
                    }
                }
            }
            nIter++
            if (!useVad && (nIter % nNewLine) == 0) {
                samplesOld = samples.takeLast(nSamplesKeep.toInt()).toMutableList()

                // Add tokens of the last full-length segment as the prompt
                if (!Config.whisperConfig.noContext) { // Assuming noContext is equivalent to params.no_context from C++
                    promptTokens.clear()
                    for (i in 0 until nSegments) {
                        val text = whisper.fullGetSegmentText(whisperContext, i)
                        promptTokens.add(text)
                    }
                }
            }
        }
    }

    private fun highPassFilter(data: FloatArray, cutoff: Float, sampleRate: Float) {
        val rc = 1.0f / (2.0f * Math.PI.toFloat() * cutoff)
        val dt = 1.0f / sampleRate
        val alpha = dt / (rc + dt)

        var y = data[0]

        for (i in 1 until data.size) {
            y = alpha * (y + data[i] - data[i - 1])
            data[i] = y
        }
    }


    private fun vadSimple(
        samples: FloatArray,
        sampleRate: Float,
        lastMs: Int,
        vadThold: Float,
        freqThold: Float,
        verbose: Boolean
    ): Boolean {
        val nSamples = samples.size
        val nSamplesLast = (sampleRate * lastMs) / 1000

        // Not enough samples - assume no speech
        if (nSamplesLast >= nSamples) {
            return false
        }

        // Optional high-pass filter could be implemented here if needed
        if (freqThold > 0.0f) {
            highPassFilter(samples, freqThold, sampleRate)
        }

        var energyAll = 0.0f
        var energyLast = 0.0f

        for (i in samples.indices) {
            val absSample = kotlin.math.abs(samples[i])
            energyAll += absSample

            if (i >= nSamples - nSamplesLast) {
                energyLast += absSample
            }
        }

        energyAll /= nSamples
        energyLast /= nSamplesLast

        return energyLast <= vadThold * energyAll
    }

    fun toTimestamp(t: Long): String {
        var sec = t / 100
        val msec = t - sec * 100
        val min = sec / 60
        sec -= min * 60
        return "%02d:%02d.%03d".format(min, sec, msec)
    }

    fun stopRecognition(): Boolean {
        audio.pause()
        return true
    }


}
