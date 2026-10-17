package com.offlineflow.tamil

/** Pure Kotlin, no platform dependencies: suitable for JVM tests. */
object DictationText {
    /** Prepare text to be inserted without altering the recognized words. */
    fun prepare(recognized: String, preceding: String?): String {
        val clean = recognized.trim()
        if (clean.isEmpty()) return ""
        val previous = preceding.orEmpty()
        // Never introduce an unnecessary space in an empty field, after whitespace,
        // or directly before punctuation (including Tamil sentence terminators).
        val needsSpace = previous.isNotEmpty() &&
            !previous.last().isWhitespace() &&
            !clean.first().toString().matches(Regex("[.,!?;:।…)]"))
        return (if (needsSpace) " " else "") + clean
    }
}
