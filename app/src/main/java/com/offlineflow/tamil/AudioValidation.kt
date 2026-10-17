package com.offlineflow.tamil

/** Pure Kotlin; lightweight diagnostics before sending PCM samples to Whisper. */
object AudioValidation {
    /** Short or essentially silent clips are rejected instead of hallucinating speech. */
    fun evaluate(samples: FloatArray, sampleRate: Int = 16_000): String? {
        if (samples.size < sampleRate / 2) return "Recording too short; speak for at least 0.5 seconds"
        if (samples.size > sampleRate * 25) return "Recording exceeds the 25-second limit"
        // RMS is less sensitive to a single transient / tap than peak amplitude.
        var power = 0.0
        for (sample in samples) power += (sample * sample).toDouble()
        val rms = kotlin.math.sqrt(power / samples.size)
        if (rms < 0.001) return "Very quiet recording; check the microphone and speak again"
        return null
    }
}
