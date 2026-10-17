package com.offlineflow.tamil

import android.Manifest
import android.app.ActivityManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.EditText
import android.text.InputType
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.DataInputStream

/** On-device setup, microphone permission and local GGML model import. */
class MainActivity : Activity() {
    private lateinit var status: TextView
    private val selectModelCode = 201
    private val microphoneCode = 202
    @Volatile private var importing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 28.dp(), 24.dp(), 18.dp())
        }
        root.addView(TextView(this).apply {
            text = "Offline Flow Tamil"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "On-device Tamil + English voice keyboard • v0.7 development build"
            textSize = 15f
        })
        status = TextView(this).apply {
            setPadding(0, 20.dp(), 0, 15.dp())
            textSize = 15f
        }
        root.addView(status)
        root.addView(button("1. Allow microphone") { askMicrophone() })
        root.addView(button("2. Import multilingual Whisper .bin model") { chooseModel() })
        root.addView(button("3. Enable keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        root.addView(button("4. Choose keyboard") {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showInputMethodPicker()
        })
        root.addView(TextView(this).apply {
            text = "5. Offline voice-typing test area"
            textSize = 17f
            setPadding(0, 20.dp(), 0, 6.dp())
        })
        val testEditor = EditText(this).apply {
            hint = "Tap here, select Offline Flow Tamil keyboard, then record:\nஇன்று நான் தமிழில் பேசுகிறேன்"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 5
            maxLines = 10
            setSingleLine(false)
            gravity = android.view.Gravity.TOP
        }
        root.addView(testEditor)
        root.addView(button("Clear test text") { testEditor.setText("") })
        root.addView(TextView(this).apply {
            text = "A model and native Whisper engine must be installed to transcribe. " +
                "This screen never sends your voice or transcript to a server. " +
                "Use Airplane Mode for an offline test."
            setPadding(0, 14.dp(), 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(root) })
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) updateStatus()
    }

    private fun updateStatus() {
        val permission = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val model = ModelStore.file(this)
        val memoryInfo = ActivityManager.MemoryInfo()
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
        val freeRamMiB = memoryInfo.availMem / 1_048_576
        status.text = "Microphone: ${if (permission) "allowed" else "not allowed"}\n" +
            "Model: ${if (model.isFile) "${model.length() / 1048576} MiB installed" else "not installed"}\n" +
            "Engine: ${if (WhisperBridge.isAvailable()) "native library available" else "not included in build"}\n" +
            "Free device RAM now: $freeRamMiB MiB (estimate)\n" +
            "Internet permission: NOT REQUESTED"
    }

    private fun button(label: String, handler: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { handler() }
    }

    private fun askMicrophone() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), microphoneCode)
        } else updateStatus()
    }

    private fun chooseModel() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, selectModelCode)
    }

    @Deprecated("Uses platform Activity callbacks to avoid external dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != selectModelCode || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        if (importing) { status.text = "Another model import is in progress"; return }
        importing = true
        status.text = "Importing your on-device model…"
        Thread {
            val destination = ModelStore.file(this)
            destination.parentFile?.mkdirs()
            val temporary = File(destination.parentFile, "model.pending")
            try {
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Could not open selected file" }
                    FileOutputStream(temporary).use { output ->
                        val chunk = ByteArray(64 * 1024)
                        var copied = 0L
                        while (true) {
                            val count = input.read(chunk)
                            if (count == -1) break
                            if (count == 0) continue
                            copied += count
                            require(copied <= 3_000_000_000L) { "Model file is too large" }
                            output.write(chunk, 0, count)
                        }
                        output.fd.sync()
                    }
                }
                require(temporary.length() > 15_000_000L) { "File is too small to be a model" }
                // Check GGML/Whisper little-endian magic 0x67676d6c: lmgg in file bytes.
                val magic = ByteArray(4)
                DataInputStream(FileInputStream(temporary)).use { it.readFully(magic) }
                require(magic.contentEquals(byteArrayOf(0x6c, 0x6d, 0x67, 0x67))) {
                    "Not a Whisper GGML model (.bin)"
                }
                WhisperBridge.replaceModel(temporary, destination)
                runOnUiThread { updateStatus() }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Model import failed: ${e.message}" }
            } finally {
                if (temporary.exists()) temporary.delete()
                importing = false
            }
        }.also { it.name = "OfflineFlow-ModelImport"; it.start() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == microphoneCode) updateStatus()
    }
    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
}
