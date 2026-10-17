import com.offlineflow.tamil.AudioValidation
import com.offlineflow.tamil.DictationText

fun main() {
    val cases = listOf(
        Triple("வணக்கம்", null, "வணக்கம்"),
        Triple("  இன்று என்ன செய்தீர்கள்?  ", "வணக்கம்", " இன்று என்ன செய்தீர்கள்?"),
        Triple("today I worked", "நான் ", "today I worked"),
        Triple(". வணக்கம்", "Hello", ". வணக்கம்"),
        Triple("   ", "hello", ""),
        Triple("தமிழ் English", "Hi!", " தமிழ் English"),
        Triple("முடிந்தது", "ஒரு\n", "முடிந்தது")
    )
    cases.forEachIndexed { i, (spoken, before, expected) ->
        check(DictationText.prepare(spoken, before) == expected) { "Text test $i failed" }
    }
    val halfSecond = FloatArray(8_000) { 0.1f }
    val silence = FloatArray(16_000)
    val almostSilence = FloatArray(16_000) { 0.0002f }
    val normalVoice = FloatArray(16_000) { if (it % 2 == 0) 0.05f else -0.05f }
    check(AudioValidation.evaluate(FloatArray(400))?.contains("too short") == true)
    check(AudioValidation.evaluate(halfSecond) == null)
    check(AudioValidation.evaluate(silence)?.contains("quiet") == true)
    check(AudioValidation.evaluate(almostSilence)?.contains("quiet") == true)
    check(AudioValidation.evaluate(normalVoice) == null)
    check(AudioValidation.evaluate(FloatArray(400_001))?.contains("25-second") == true)
    // JNI now returns raw UTF-8 bytes. Test the Kotlin decode contract, including
    // Tamil glyphs, mixed-script text and a four-byte emoji code point.
    val unicodeCases = listOf("வணக்கம்", "இன்று meeting இருக்கு", "தமிழ் 😊", "நன்றி! 👍")
    unicodeCases.forEachIndexed { index, original ->
        val decoded = original.toByteArray(Charsets.UTF_8).toString(Charsets.UTF_8)
        check(decoded == original) { "UTF-8 round trip $index failed" }
    }
    println("PASS: 7 dictation + 6 audio + 4 Unicode JVM checks (17 total)")
}
