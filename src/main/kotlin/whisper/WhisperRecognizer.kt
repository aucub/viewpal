package whisper

import Singleton
import config.Config
import io.github.givimad.whisperjni.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Paths
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class WhisperRecognizer {
    companion object {
        private val job = Job()
        val scope = CoroutineScope(Dispatchers.Default + job)
        val nThreads = minOf(Config.config.whisperConfig.nThreads, Runtime.getRuntime().availableProcessors())
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
        lateinit var samples: FloatArray
        var useVad = false
        var nNewLine = 1
        var promptTokens: String? = Config.config.whisperConfig.initialPrompt
        var tLast = 0
        var tFirst = 0
    }

    init {
        Config.config.whisperConfig.keepMs = min(Config.config.whisperConfig.keepMs, Config.config.whisperConfig.stepMs)
        Config.config.whisperConfig.lengthMs =
            max(Config.config.whisperConfig.lengthMs, Config.config.whisperConfig.stepMs)
        nSamplesStep = Config.config.whisperConfig.stepMs * 1e-3 * Config.config.whisperConfig.sampleRate
        nSamplesLen = Config.config.whisperConfig.lengthMs * 1e-3 * Config.config.whisperConfig.sampleRate
        nSamplesKeep = Config.config.whisperConfig.keepMs * 1e-3 * Config.config.whisperConfig.sampleRate
        nSamples30s = 30000.0 * 1e-3 * Config.config.whisperConfig.sampleRate
        useVad = nSamplesStep <= 0
        nNewLine =
            if (!useVad) max(1, Config.config.whisperConfig.lengthMs / Config.config.whisperConfig.stepMs - 1) else 1
        whisperFullParams.nThreads = nThreads
        whisperFullParams.noTimestamps = !useVad
        whisperFullParams.noContext = Config.config.whisperConfig.noContext || useVad
        whisperFullParams.printProgress = false
        whisperFullParams.printSpecial = Config.config.whisperConfig.printSpecial
        whisperFullParams.printRealtime = false
        whisperFullParams.printTimestamps = !Config.config.whisperConfig.noTimestamps
        whisperFullParams.singleSegment = !useVad
        whisperFullParams.translate = Config.config.whisperConfig.translate
        whisperFullParams.audioCtx = Config.config.whisperConfig.audioCtx
        whisperFullParams.speedUp = Config.config.whisperConfig.speedUp
        whisperFullParams.language = Config.config.whisperConfig.language
        whisperFullParams.initialPrompt = Config.config.whisperConfig.initialPrompt
        whisperFullParams.temperatureInc =
            if (Config.config.whisperConfig.noFallback) 0.0f else whisperFullParams.temperatureInc
        val loadOptions =
            WhisperJNI.LoadOptions().apply {
                logger = WhisperJNI.LibraryLogger { println(it) }
                whisperLib = Paths.get(Config.config.whisperConfig.whisperLib)
            }
        WhisperJNI.loadLibrary(loadOptions)
        WhisperJNI.setLibraryLogger(null)
        whisper = WhisperJNI()
        val whisperContextParams = WhisperContextParams()
        whisperContextParams.useGPU = Config.config.whisperConfig.useGPU
        whisperContext = whisper.init(Paths.get(Config.config.whisperConfig.model), whisperContextParams)
        if (!whisper.isMultilingual(whisperContext)) {
            if (Config.config.whisperConfig.translate) {
                Config.config.whisperConfig.translate = false
                System.err.println("警告：模型不是多语言的，忽略语言和翻译选项")
            }
        }
        val grammar = whisper.parseGrammar(File("src/main/resources/view-zh.gbnf").readText())
        whisperFullParams.grammar = grammar
        whisperFullParams.grammarPenalty = 100f
    }

    fun startRecognition() =
        scope.launch {
            isRunning = true
            var nIter = 0
            while (isRunning) {
                if (!isRunning) {
                    break
                }
                val samplesArray = Singleton.audio.get()
                if (samplesArray == null) {
                    Thread.sleep(Config.config.whisperConfig.delayMs * Random.nextInt(1, 11))
                    continue
                }
                samplesNew = samplesArray.toMutableList()
                tLast = (tLast + samplesNew.size / Config.config.whisperConfig.sampleRate * 2).toInt()
                if (!useVad) {
                    val nSamplesNew = samplesNew.size
                    val nSamplesTake = min(samplesOld.size, max(0, (nSamplesKeep + nSamplesLen - nSamplesNew).toInt()))
                    samples = FloatArray(nSamplesNew + nSamplesTake)
                    for (i in 0 until nSamplesTake) {
                        samples[i] = samplesOld[samplesOld.size - nSamplesTake + i]
                    }
                    for (i in 0 until nSamplesNew) {
                        samples[nSamplesTake + i] = samplesNew[i]
                    }
                    samplesOld = samples.toMutableList()
                    tFirst = (tLast - (nSamplesNew + nSamplesTake) / Config.config.whisperConfig.sampleRate * 2).toInt()
                } else {
                    if (vadSimple(
                            samplesNew.toFloatArray(),
                            Config.config.whisperConfig.sampleRate,
                            1000,
                            Config.config.whisperConfig.vadThold,
                            Config.config.whisperConfig.freqThold,
                        )
                    ) {
                        samples = samplesArray
                        tFirst = (tLast - samples.size / Config.config.whisperConfig.sampleRate * 2).toInt()
                    } else {
                        Thread.sleep(Config.config.whisperConfig.delayMs * Random.nextInt(1, 11))
                        continue
                    }
                }
                if (whisper.full(whisperContext, whisperFullParams, samples, samples.size) != 0) {
                    println("音频处理失败")
                    return@launch
                }
                val nSegments = whisper.fullNSegments(whisperContext)
                if (nSegments > 0) {
                    for (i in 0 until nSegments) {
                        val text = whisper.fullGetSegmentText(whisperContext, i)
                        if (whisperFullParams.noTimestamps) {
                            Segment.add(Segment(text, tFirst, tLast))
                        } else {
                            val t0 = whisper.fullGetSegmentTimestamp0(whisperContext, i) / 100
                            val t1 = whisper.fullGetSegmentTimestamp1(whisperContext, i) / 100
                            Segment.add(Segment(text, (tFirst + t0).toInt(), (tFirst + t1).toInt()))
                        }
                    }
                }
                nIter++
                if (!useVad && (nIter % nNewLine) == 0) {
                    samplesOld = samples.takeLast(nSamplesKeep.toInt()).toMutableList()
                    if (!Config.config.whisperConfig.noContext) {
                        for (i in 0 until nSegments) {
                            val text = whisper.fullGetSegmentText(whisperContext, i)
                            promptTokens += text
                        }
                    }
                }
                Thread.sleep(Config.config.whisperConfig.delayMs)
            }
        }

    private fun highPassFilter(
        data: FloatArray,
        cutoff: Float,
        sampleRate: Float,
    ) {
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
    ): Boolean {
        val nSamples = samples.size
        val nSamplesLast = (sampleRate * lastMs) / 1000

        if (nSamplesLast >= nSamples) {
            return false
        }

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

    fun stopRecognition(): Boolean {
        isRunning = false
        return true
    }
}
