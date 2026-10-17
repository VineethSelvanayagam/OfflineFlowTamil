package com.offlineflow.tamil

import android.content.Context
import java.io.File

object ModelStore {
    fun file(context: Context): File = File(context.filesDir, "models/ggml-model.bin")
}
