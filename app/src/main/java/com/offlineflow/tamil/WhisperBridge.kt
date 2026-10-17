package com.offlineflow.tamil

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** JNI bridge: all interactions are serialized, including model replacement. */
object WhisperBridge {
    private var loaded = false
    private var context = 0L

    @Synchronized fun isAvailable(): Boolean {
        if (loaded) return true
        return try {
            System.loadLibrary("offlinewhisper")
            loaded = true
            true
        } catch (_: UnsatisfiedLinkError) { false }
    }

    @Synchronized fun transcribe(model: File, samples: FloatArray, language: String): String {
        check(isAvailable()) { "Whisper engine is not in this build" }
        check(model.isFile) { "Import a multilingual Whisper .bin model first" }
        require(language in setOf("ta", "en", "auto")) { "Unsupported language" }
        if (context == 0L) {
            context = nativeLoad(model.absolutePath)
            check(context != 0L) { "Could not load model: confirm it is multilingual GGML, not .en.bin, and check RAM" }
        }
        return nativeTranscribeUtf8(context, samples, language).toString(Charsets.UTF_8)
    }

    /** The old model is retained unless copying and validation succeeded earlier. */
    @Synchronized fun replaceModel(temporary: File, destination: File) {
        check(temporary.isFile) { "No imported file to activate" }
        check(isAvailable()) { "Offline Whisper engine must be installed before importing a model" }
        // Validate GGML data, available RAM, and multilingual capability before
        // replacing a previously working model. A magic-byte check alone is not enough.
        release()
        val candidateContext = nativeLoad(temporary.absolutePath)
        check(candidateContext != 0L) {
            "Cannot load this multilingual Whisper model; check file and available RAM"
        }
        nativeRelease(candidateContext)
        try {
            Files.move(temporary.toPath(), destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    @Synchronized fun release() {
        if (context != 0L && loaded) nativeRelease(context)
        context = 0L
    }

    private external fun nativeLoad(path: String): Long
    /** UTF-8 bytes (not JNI modified UTF-8): safely preserves Tamil and emoji. */
    private external fun nativeTranscribeUtf8(handle: Long, samples: FloatArray, language: String): ByteArray
    private external fun nativeRelease(handle: Long)
}
