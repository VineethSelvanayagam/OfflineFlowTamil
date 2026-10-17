# Offline Flow Tamil v0.7 — Android offline dictation keyboard

**Development source code, not an APK.** There is no cloud service or monthly API fee for on-device transcription. The final phone app never asks for Android INTERNET permission. Developer build setup and fetching a Whisper model may use the internet before offline usage.

## v0.7 milestone: ready-to-upload GitHub Actions source

- Pushing all source files to a dedicated GitHub `main` branch automatically attempts an Android build. See `GITHUB_APK_BUILD_TA.md`.
- The local model import now verifies that the selected model loads and is multilingual **before** replacing a previously imported model.
- The build *always* includes the `externalNativeBuild` CMake step; the native whisper.cpp speech engine is no longer optional. A build that lacks the vendored source must fail rather than produce a non-dictating keyboard shell.
- GitHub Actions checks that an APK contains `lib/arm64-v8a/libofflinewhisper.so`, and that APK permissions include microphone permission but NOT INTERNET permission.
- The JNI `nativeLoad` rejects English-only `.en` models: a **multilingual** GGML Whisper model is required for Tamil.
- JNI model/language Java string accesses now check for null on allocation failure.
- The remaining design is the v0.5 25s, user-controlled AudioRecord + Android InputMethodService + native Whisper integration with a local test text field.

## How to build

See **BUILD_AND_TEST_TA.md** (Tamil). Requires Android Studio or Android SDK+NDK+CMake+Gradle plus upstream pinned whisper.cpp source. Native source is deliberately not distributed in this ZIP; run either setup script on a development computer first. Use Whisper GGML `ggml-base.bin` (not `ggml-base.en.bin`) after installing the APK.

## What has *not* been proven

- The entire Android APK has NOT compiled in this environment (Android SDK/NDK/Gradle absent).
- Whisper.cpp v1.9.4 native library has NOT been built against a real Android NDK here.
- No Android device/audio recognition/accuracy/latency/battery measurement.
- It is NOT yet comparable with Wispr Flow smart editing and streaming dictation.

## Privacy and limitations

No `android.permission.INTERNET`, no voice/upload telemetry, no remote AI API. Users can opt into selecting the keyboard; password fields disable microphone capture. Transcripts are only committed to the currently focused editable field; that third-party app can independently process submitted text according to its own practices. Audio stays in process memory and is not intentionally persisted. This is a prototype; security/privacy need on-device verification before public release.

For source provenance and license, see https://github.com/ggml-org/whisper.cpp (MIT). Model download and licensing depend on the chosen weights distributor.
