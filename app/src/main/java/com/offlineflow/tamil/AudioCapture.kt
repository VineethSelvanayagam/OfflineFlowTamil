package com.offlineflow.tamil

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream

/** 16kHz mono PCM16, user-started, memory-only, maximum 25 seconds. */
class AudioCapture(private val onMaxDuration: () -> Unit = {}) {
    companion object {
        const val SAMPLE_RATE = 16_000
        const val MAX_SECONDS = 25
    }
    private val maxBytes = SAMPLE_RATE * 2 * MAX_SECONDS
    @Volatile private var recording = false
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    private val audio = ByteArrayOutputStream()

    @Synchronized fun start() {
        check(!recording && recorder == null) { "Already recording" }
        val min = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(min > 0) { "16 kHz audio is not available" }
        val mic = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min * 2, 4096))
        if (mic.state != AudioRecord.STATE_INITIALIZED) {
            mic.release()
            error("Could not initialize the microphone")
        }
        synchronized(audio) { audio.reset() }
        try { mic.startRecording() } catch (e: Exception) { mic.release(); throw e }
        recorder = mic
        recording = true
        worker = Thread {
            val chunk = ByteArray(4096)
            var hitLimit = false
            try {
                while (recording) {
                    val n = mic.read(chunk, 0, chunk.size)
                    if (n <= 0) break
                    synchronized(audio) {
                        val available = maxBytes - audio.size()
                        if (available > 0) audio.write(chunk, 0, minOf(available, n))
                        if (audio.size() >= maxBytes) {
                            recording = false
                            hitLimit = true
                        }
                    }
                }
            } finally {
                if (hitLimit) onMaxDuration()
            }
        }.also { it.name = "OfflineFlow-Microphone"; it.start() }
    }

    /** Must run on a worker thread. Do not call stop and discard concurrently. */
    @Synchronized fun stop(): FloatArray {
        recording = false
        val mic = recorder
        try { mic?.stop() } catch (_: IllegalStateException) { }
        worker?.join(3000)
        mic?.release()
        worker = null
        recorder = null
        val bytes = synchronized(audio) { audio.toByteArray().also { audio.reset() } }
        return FloatArray(bytes.size / 2) { i ->
            val lo = bytes[i * 2].toInt() and 0xff
            val hi = bytes[i * 2 + 1].toInt() and 0xff
            ((hi shl 8) or lo).toShort().toInt() / 32768.0f
        }
    }

    fun discard() {
        stop().fill(0f)
    }
}
