package ch.inf.usi.mindbricks.drivers;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.RequiresPermission;

public class MicrophoneRecorder {
    private static final String LOG_TAG = "Recorder";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private final int bufferSize;
    private Thread recordingThread = null;
    private volatile boolean isRecording = false;
    private volatile double currentAmplitude = 0;

    public MicrophoneRecorder() {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public void startRecording() {
        if (isRecording) {
            return;
        }

        if (bufferSize <= 0) {
            Log.e(LOG_TAG, "Cannot start recording: invalid buffer/sample rate");
            return;
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "AudioRecord initialization failed");
            return;
        }

        audioRecord.startRecording();
        isRecording = true;
        Log.d(LOG_TAG, "Recording started. Rate=" + SAMPLE_RATE + "Hz buffer=" + bufferSize + " state=" + audioRecord.getRecordingState());

        recordingThread = new Thread(this::readAudioData, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void readAudioData() {
        short[] audioBuffer = new short[bufferSize / 2];

        while (isRecording) {
            int resultSize = audioRecord.read(audioBuffer, 0, audioBuffer.length);

            if (resultSize > 0) {
                calculateRMS(audioBuffer, resultSize);
            } else if (resultSize < 0) {
                Log.e(LOG_TAG, "Error reading audio: " + resultSize);
            } else {
                Log.w(LOG_TAG, "AudioRecord read returned 0 bytes");
            }
        }
    }

    /**
     * Calculates the Root Mean Square over the audio buffer.
     * <p>
     * RMS allows to estimate loudness over short time windows.
     * <p>
     * SOURCES:
     * - <a href="https://en.wikipedia.org/wiki/DBFS">...</a>
     *      - states that RMS is used for loudness measurement
     *      FIXME: this reference is not used currently in the code, I should remove it later
     * - <a href="https://discourse.ardour.org/t/calculating-rms-in-digital-audio/109812">...</a>
     *      - details the RMS calculation
     *
     * @param buffer audio samples
     * @param readSize number of valid samples in buffer
     */
    private void calculateRMS(short[] buffer, int readSize) {
        // compute sum_i x_i^2
        double sum = 0;
        for (int i = 0; i < readSize; i++) {
            sum += buffer[i] * buffer[i];
        }

        if (readSize > 0) {
            // compute RMS:
            // A_rms = sqrt(sum_i x_i^2 / N)
            currentAmplitude = Math.sqrt(sum / readSize);
        }
    }

    public double getCurrentAmplitude() {
        return currentAmplitude;
    }

    public void stopRecording() {
        isRecording = false;

        if (audioRecord != null) {
            try {
                // wait for the recording thread to finish before releasing resources
                if (recordingThread != null) {
                    recordingThread.join();
                }
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Interrupted while waiting for thread to finish");
            }

            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
            }
            audioRecord.release();
            audioRecord = null;
        }

        recordingThread = null;
    }
}
