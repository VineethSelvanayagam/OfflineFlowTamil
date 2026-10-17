#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include "whisper.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_offlineflow_tamil_WhisperBridge_nativeLoad(
    JNIEnv *env, jobject, jstring jpath) {
    if (!jpath) return 0;
    const char *raw = env->GetStringUTFChars(jpath, nullptr);
    if (!raw) return 0;
    std::string path(raw);
    env->ReleaseStringUTFChars(jpath, raw);
    whisper_context_params contextParams = whisper_context_default_params();
    contextParams.use_gpu = false; // CPU-only: work on Android without a GPU backend.
    whisper_context *context = whisper_init_from_file_with_params(path.c_str(), contextParams);
    if (context && !whisper_is_multilingual(context)) {
        __android_log_print(ANDROID_LOG_WARN, "OfflineFlowTamil",
            "Selected model is English-only, but multilingual Tamil support is required");
        whisper_free(context);
        return 0;
    }
    return reinterpret_cast<jlong>(context);
}

// Return ordinary UTF-8 as a byte array: NewStringUTF uses *modified* UTF-8,
// which does not accept all four-byte Unicode sequences (e.g., emoji).
static jbyteArray makeUtf8Bytes(JNIEnv *env, const std::string &value) {
    jbyteArray arr = env->NewByteArray(static_cast<jsize>(value.size()));
    if (!arr) return nullptr;
    if (!value.empty()) {
        env->SetByteArrayRegion(arr, 0, static_cast<jsize>(value.size()),
                                reinterpret_cast<const jbyte *>(value.data()));
    }
    return arr;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_offlineflow_tamil_WhisperBridge_nativeTranscribeUtf8(
    JNIEnv *env, jobject, jlong handle, jfloatArray jaudio, jstring jlanguage) {
    auto *context = reinterpret_cast<whisper_context *>(handle);
    if (!context) return makeUtf8Bytes(env, "");
    if (!jaudio || !jlanguage) return makeUtf8Bytes(env, "");
    jsize length = env->GetArrayLength(jaudio);
    if (length < 8000 || length > (16000 * 25)) return makeUtf8Bytes(env, "");
    std::vector<float> samples(static_cast<size_t>(length));
    env->GetFloatArrayRegion(jaudio, 0, length, samples.data());
    const char *raw = env->GetStringUTFChars(jlanguage, nullptr);
    if (!raw) return makeUtf8Bytes(env, "");
    std::string language(raw);
    env->ReleaseStringUTFChars(jlanguage, raw);

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.language = language.c_str();
    params.n_threads = 4;
    params.no_context = true;
    params.no_timestamps = true;
    params.translate = false; // Separate snippets must not leak earlier dictation context.
    params.single_segment = false;
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    int result = whisper_full(context, params, samples.data(), static_cast<int>(samples.size()));
    if (result != 0) return makeUtf8Bytes(env, "");

    std::string text;
    int segments = whisper_full_n_segments(context);
    for (int i = 0; i < segments; i++) {
        const char *segment = whisper_full_get_segment_text(context, i);
        if (segment) text += segment;
    }
    return makeUtf8Bytes(env, text);
}

extern "C" JNIEXPORT void JNICALL
Java_com_offlineflow_tamil_WhisperBridge_nativeRelease(
    JNIEnv *, jobject, jlong handle) {
    auto *context = reinterpret_cast<whisper_context *>(handle);
    if (context) whisper_free(context);
}
