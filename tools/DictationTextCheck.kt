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
        val got = DictationText.prepare(spoken, before)
        check(got == expected) { "Case $i: got <$got> expected <$expected>" }
    }
    println("PASS: ${cases.size} pure Kotlin dictation formatting checks")
}
