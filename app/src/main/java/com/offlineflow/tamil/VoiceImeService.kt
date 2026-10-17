package com.offlineflow.tamil

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** User-controlled offline IME. No transcript history, analytics or network calls. */
class VoiceImeService : InputMethodService() {
    private val handler = Handler(Looper.getMainLooper())
    private var capture: AudioCapture? = null
    private var language = "ta"
    private var status: TextView? = null
    private var micButton: Button? = null
    private var securedField = false
    private var generation = 0
    // Only the most recent asynchronous record/stop task may update keyboard UI.
    private var operationSerial = 0
    @Volatile private var busy = false

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12.dp(), 8.dp(), 12.dp(), 12.dp())
        }
        root.addView(TextView(this).apply {
            text = "Offline Flow Tamil"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        })
        status = TextView(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, 7.dp(), 0, 7.dp())
            textSize = 13f
        }.also { root.addView(it) }
        val topRow = LinearLayout(this)
        micButton = addWeightedButton(topRow, "🎤 Record") { toggleRecording() }
        addWeightedButton(topRow, "தமிழ்") { button ->
            language = when (language) { "ta" -> "en"; "en" -> "auto"; else -> "ta" }
            button.text = when (language) { "ta" -> "தமிழ்"; "en" -> "English"; else -> "Auto" }
        }
        root.addView(topRow)
        val bottomRow = LinearLayout(this)
        addWeightedButton(bottomRow, "Space") { currentInputConnection?.commitText(" ", 1) }
        addWeightedButton(bottomRow, "⌫") {
            currentInputConnection?.deleteSurroundingTextInCodePoints(1, 0)
        }
        addWeightedButton(bottomRow, "↵") { currentInputConnection?.commitText("\n", 1) }
        addWeightedButton(bottomRow, "⌨") {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showInputMethodPicker()
        }
        root.addView(bottomRow)
        updateState()
        return root
    }

    /** onStartInput may fire without the keyboard view being re-created. */
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        invalidateField()
        securedField = isPassword(attribute?.inputType ?: 0)
        updateState()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        invalidateField()
        securedField = isPassword(info?.inputType ?: 0)
        updateState()
    }

    override fun onFinishInput() {
        invalidateField()
        super.onFinishInput()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        invalidateField()
        super.onFinishInputView(finishingInput)
    }

    private fun invalidateField() {
        generation++
        if (capture != null) cancelRecording()
    }

    private fun isPassword(inputType: Int): Boolean {
        val klass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return (klass == InputType.TYPE_CLASS_TEXT && variation in setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
            (klass == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
    }

    private fun updateState(message: String? = null) {
        micButton?.isEnabled = !busy && !securedField
        micButton?.text = if (capture != null) "⏹ Stop" else "🎤 Record"
        status?.text = message ?: when {
            securedField -> "Password field: microphone disabled"
            busy -> "Working locally…"
            capture != null -> "Recording locally (max 25s)"
            else -> "Tap Record to dictate offline"
        }
    }

    private fun toggleRecording() {
        if (securedField || busy) return
        if (capture != null) { finishRecording(); return }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            updateState("Grant microphone permission inside the app")
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        if (!WhisperBridge.isAvailable()) { updateState("Whisper engine not built in") ; return }
        if (!ModelStore.file(this).isFile) { updateState("Import multilingual Whisper model first") ; return }
        try {
            lateinit var active: AudioCapture
            active = AudioCapture {
                handler.post {
                    if (capture === active) finishRecording()
                }
            }
            capture = active
            active.start()
            updateState()
        } catch (e: Exception) {
            capture = null
            updateState("Microphone unavailable: ${e.message}")
        }
    }

    private fun finishRecording() {
        val audio = capture ?: return
        capture = null
        busy = true
        val serial = ++operationSerial
        val originGeneration = generation
        val selectedLanguage = language
        updateState("Transcribing locally…")
        Thread {
            try {
                val pcm = audio.stop()
                val result = try {
                    val validationError = AudioValidation.evaluate(pcm)
                    check(validationError == null) { validationError ?: "Invalid recording" }
                    WhisperBridge.transcribe(ModelStore.file(this), pcm, selectedLanguage)
                } finally {
                    pcm.fill(0f)
                }
                handler.post {
                    if (serial != operationSerial) return@post
                    busy = false
                    if (originGeneration == generation && !securedField && isInputViewShown) {
                        val connection = currentInputConnection
                        val before = connection?.getTextBeforeCursor(1, 0)?.toString()
                        val insert = DictationText.prepare(result, before)
                        if (insert.isNotEmpty()) {
                            connection?.commitText(insert, 1)
                            updateState("Inserted ${insert.length} characters locally")
                        } else updateState("No speech recognized")
                    } else updateState("Discarded: input field changed")
                    micButton?.isEnabled = !securedField
                }
            } catch (e: Exception) {
                handler.post {
                    if (serial == operationSerial) {
                        busy = false
                        updateState("Recognition error: ${e.message}")
                    }
                }
            }
        }.also { it.name = "OfflineFlow-Whisper"; it.start() }
    }

    private fun cancelRecording() {
        val old = capture ?: return
        capture = null
        val serial = ++operationSerial
        busy = true
        updateState("Stopping microphone…")
        Thread {
            old.discard()
            handler.post {
                if (serial == operationSerial) {
                    busy = false
                    updateState("Recording discarded")
                }
            }
        }.also { it.name = "OfflineFlow-Cancel"; it.start() }
    }

    override fun onDestroy() {
        operationSerial++
        if (capture != null) cancelRecording()
        // Background ASR holds a synchronized native handle, which is safely
        // reclaimed on model switch or when the IME process terminates.
        super.onDestroy()
    }

    private fun addWeightedButton(row: LinearLayout, name: String, action: (Button) -> Unit): Button {
        val button = Button(this).apply {
            text = name
            isAllCaps = false
            textSize = 12f
            minWidth = 0
            minimumWidth = 0
            setPadding(1.dp(), 0, 1.dp(), 0)
            setOnClickListener { action(this) }
        }
        row.addView(button, LinearLayout.LayoutParams(0, 52.dp(), 1f))
        return button
    }
    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
}
