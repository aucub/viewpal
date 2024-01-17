package whisper;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WhisperRecognizer {

    private static WhisperJNI whisper;

    private static WhisperContext whisperContext;

    private TargetDataLine line;

    private boolean isRecording = false;

    public WhisperRecognizer() throws IOException {
        var loadOptions = new WhisperJNI.LoadOptions();
        loadOptions.logger = System.out::println;
        loadOptions.whisperLib = Paths.get("/usr/lib/libwhisper.so");
        WhisperJNI.loadLibrary(loadOptions);
        WhisperJNI.setLibraryLogger(null);
        this.whisper = new WhisperJNI();
        whisperContext = whisper.init(Path.of("/usr/share/whisper.cpp-model-tiny/tiny.bin"));
    }

    public void startRecognition(RecognitionListener listener) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        isRecording = true;
        // 开启录音线程
        new Thread(() -> {
            while (isRecording) {
                float[] data = getLatestData();
                String text = recognize(data);
                listener.onResult(text);
            }
        }).start();
    }


    private float[] getLatestData() {
        byte[] buffer = new byte[1024];
        int bytesRead = line.read(buffer, 0, buffer.length);
        if (bytesRead > 0) {
            // 避免数据复制,直接转换
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
            // 预分配FloatBuffer
            FloatBuffer floatBuffer = FloatBuffer.allocate(shortBuffer.capacity());
            // 直接转换到FloatBuffer
            while (shortBuffer.hasRemaining()) {
                floatBuffer.put(shortBuffer.get() / 32768.0f);
            }
            return floatBuffer.array();
        }

        return new float[0];

    }


    public void stopRecognition() {
        isRecording = false;
        line.stop();
        line.close();
    }

    private String recognize(float[] data) {
        String text = "";
        try {
            var params = new WhisperFullParams();
            int result = whisper.full(whisperContext, params, data, data.length);
            if (result != 0) {
                throw new IOException("Transcription failed with code " + result);
            }
            text = whisper.fullGetSegmentText(whisperContext, 0);
            int numSegments = whisper.fullNSegments(whisperContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }

    public interface RecognitionListener {
        void onResult(String text);
    }

}
