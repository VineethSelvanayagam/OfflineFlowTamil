#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <cmath>
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
    contextParams.use_gpu = false; // Runs offline on ARM64 CPUs.
    whisper_context *context = whisper_init_from_file_with_params(path.c_str(), contextParams);
    if (context && !whisper_is_multilingual(context)) {
        __android_log_print(ANDROID_LOG_WARN, "OfflineFlowTamil",
            "English-only model rejected; multilingual model is required");
        whisper_free(context);
        return 0;
    }
    return reinterpret_cast<jlong>(context);
}

// Ordinary UTF-8 in a byte array preserves the full Unicode range,
// including Tamil script and emoji; JNI modified UTF-8 is not suitable.
static jbyteArray makeUtf8Bytes(JNIEnv *env, const std::string &value) {
    jbyteArray arr = env->NewByteArray(static_cast<jsize>(value.size()));
    if (!arr) return nullptr;
    if (!value.empty()) {
        env->SetByteArrayRegion(arr, 0, static_cast<jsize>(value.size()),
                                reinterpret_cast<const jbyte *>(value.data()));
    }
    return arr;
}

struct RecognitionCandidate {
    std::string text;
    double avgLogProbability = -100.0;
    int scoredTokens = 0;
};

static RecognitionCandidate recognizeInLanguage(
    whisper_context *context, const std::vector<float> &samples,
    const char *language) {
    RecognitionCandidate candidate;
    // Beam search is a measured quality/speed tradeoff to test on phones.
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_BEAM_SEARCH);
    params.language = language;
    params.beam_search.beam_size = 3;
    params.n_threads = 4;
    params.no_context = true;
    params.no_timestamps = true;
    params.translate = false;
    params.suppress_nst = true;
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    if (whisper_full(context, params, samples.data(),
                     static_cast<int>(samples.size())) != 0) {
        return candidate;
    }

    const int segments = whisper_full_n_segments(context);
    const whisper_token eot = whisper_token_eot(context);
    double logProbabilitySum = 0.0;
    for (int i = 0; i < segments; ++i) {
        const char *segment = whisper_full_get_segment_text(context, i);
        if (segment) candidate.text += segment;
        const int tokens = whisper_full_n_tokens(context, i);
        for (int j = 0; j < tokens; ++j) {
            const whisper_token tokenId = whisper_full_get_token_id(context, i, j);
            // Only score ordinary text tokens, not EOT/language/task markers.
            if (tokenId >= eot) continue;
            const float p = whisper_full_get_token_p(context, i, j);
            if (!std::isfinite(p) || p <= 0.f) continue;
            logProbabilitySum += std::log(std::max(0.000001f, p));
            candidate.scoredTokens++;
        }
    }
    if (candidate.scoredTokens > 0) {
        candidate.avgLogProbability = logProbabilitySum / candidate.scoredTokens;
    }
    return candidate;
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
    if (env->ExceptionCheck()) return nullptr;
    const char *raw = env->GetStringUTFChars(jlanguage, nullptr);
    if (!raw) return makeUtf8Bytes(env, "");
    std::string language(raw);
    env->ReleaseStringUTFChars(jlanguage, raw);

    if (language == "en" || language == "ta") {
        return makeUtf8Bytes(env, recognizeInLanguage(
            context, samples, language.c_str()).text);
    }
    if (language != "auto") return makeUtf8Bytes(env, "");

    // v0.7 passed "auto" directly to Whisper, which can misclassify
    // English as Tamil on short clips. In auto mode, constrain choices to
    // English and Tamil, decoding both, then compare mean text-token confidence.
    // Each pass resets context, and decoded text NEVER goes to logcat.
    const RecognitionCandidate english = recognizeInLanguage(context, samples, "en");
    const RecognitionCandidate tamil = recognizeInLanguage(context, samples, "ta");
    bool useEnglish = false;
    if (tamil.text.empty()) {
        useEnglish = true;
    } else if (!english.text.empty()) {
        useEnglish = english.avgLogProbability >= tamil.avgLogProbability;
    }
    const RecognitionCandidate &chosen = useEnglish ? english : tamil;
    __android_log_print(ANDROID_LOG_INFO, "OfflineFlowTamil",
        "Auto language compared local candidates; selected %s (scores hidden)",
        useEnglish ? "en" : "ta");
    return makeUtf8Bytes(env, chosen.text);
}

extern "C" JNIEXPORT void JNICALL
Java_com_offlineflow_tamil_WhisperBridge_nativeRelease(
    JNIEnv *, jobject, jlong handle) {
    auto *context = reinterpret_cast<whisper_context *>(handle);
    if (context) whisper_free(context);
}
