# v0.7 Release Gate Checklist

- [x] Source has no Android INTERNET permission.
- [x] CMake Whisper native integration is mandatory, not an opt-in Gradle flag.
- [x] Multilingual model enforcement in JNI `nativeLoad`.
- [x] GitHub Actions checks packaged native library and APK permissions.
- [ ] Android SDK/NDK/Gradle successful native APK compilation.
- [ ] APK install and microphone permission flow on actual phone.
- [ ] Multilingual GGML model import and model load.
- [ ] Airplane Mode Tamil transcription into local editor.
- [ ] Airplane Mode English and mixed-language dictation tests.
- [ ] Runtime permissions, password field, focus-change, thermal and battery testing.
- [ ] Public release security/privacy review.

Successful static tests do not satisfy the physical-device release gate.
