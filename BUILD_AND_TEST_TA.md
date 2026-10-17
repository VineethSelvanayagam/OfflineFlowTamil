# Offline Flow Tamil v0.7 — Build & Test (தமிழ்)

**நிலை:** Source Project மட்டுமே. சோதிக்கப்பட்ட APK அல்ல. உண்மையான Android Build வெற்றிக்கு இப்போது Whisper.cpp Engine கட்டாயம். Engine இல்லாமல் Build செய்ய முயன்றால் Build தோல்வி அடைவது திட்டமிட்ட பாதுகாப்பு.

## கணினியில் தேவையானவை

- Android Studio + JDK 17
- Android SDK Platform 35, Build Tools 35.0.0
- Android NDK 26.3.11579264, CMake 3.22.1
- Gradle 8.9 — இந்த ZIP-ல் Gradle Wrapper இல்லை; Android Studio தானாக ஆதரிக்கக்கூடிய Gradle நிறுவலை அமைக்க வேண்டும் அல்லது Gradle 8.9 நிறுவ வேண்டும்
- Internet: ஆரம்பத்தில் developer source மற்றும் GGML model பெறுவதற்கே. **மொபைலில் செயலி இயங்க இணையம் தேவைப்படாது.**
- Android 9+ ARM64 மொபைல்

## Build — macOS / Linux

1. ZIP Extract செய்து `OfflineFlowTamil_v0.7` directory-க்குச் செல்லவும்.
2. Android Studio SDK Manager-இல் மேலுள்ள SDK/NDK/CMake version-களை நிறுவவும்.
3. Terminal: `bash setup_native_macos_linux.sh` (whisper.cpp v1.9.4 source பெறப்படும்).
4. `bash verify_build_environment.sh` இயக்கி குறைந்தபட்ச சோதனைகளைப் பாருங்கள்.
5. Android Studio-வில் project-ஐ Open செய்து Gradle Sync செய்யவும். பின்னர் `Build > Build APK(s)` அல்லது terminal-ல் `gradle :app:assembleDebug`.
6. வெற்றி பெற்றால் `app/build/outputs/apk/debug/app-debug.apk` உருவாகும்.
7. `aapt dump permissions app/build/outputs/apk/debug/app-debug.apk` மூலம் `INTERNET` permission இல்லை என்பதைச் சரிபார்க்கவும். `unzip -l ...` மூலம் `lib/arm64-v8a/libofflinewhisper.so` உள்ளதா உறுதிப்படுத்தவும்.

## Build — Windows

1. PowerShell-ல் Source folder-இலிருந்து `./setup_native_windows.ps1` இயக்கவும் (Git நிறுவப்பட்டிருக்க வேண்டும்).
2. Android Studio SDK Manager-இல் தேவையான packages நிறுவவும்.
3. Gradle Sync, `Build > Build APK(s)`; சில Windows NDK/CMake path சிக்கல்களை தனியே சரிசெய்ய வேண்டி இருக்கலாம்.

## GitHub Actions மூலம் முயற்சி

1. Project-இன் **உள்ளே உள்ள எல்லா கோப்புகளையும்**, குறிப்பாக `.github/workflows/android-debug.yml`, தனிப்பட்ட GitHub repository-க்குள் Root-இல் சேர்க்கவும்.
2. Repository > Actions > **Android Offline Flow Tamil — Debug APK** > Run workflow.
3. Workflow `whisper.cpp` source-ஐ developer-build runner-ல் பெறும்; Gradle Build செய்வது, APK உள்ளே Native `.so` இருக்கிறதா, INTERNET permission இல்லையா என்பதைச் சோதிக்கும்.
4. Workflow வெற்றி பெற்றால் Run-ன் Artifacts பகுதியில் **unverified debug APK** கிடைக்கும். இது மொபைலில் Speech Recognition சோதிக்கப்பட்டது என்று அர்த்தமில்லை.
5. Error இருந்தால் அந்த Build Log-இன் முதல் error message-ஐ ஆய்வு செய்ய வேண்டும்.

## மொபைலில் பரிசோதனை

1. Debug APK நிறுவியபின் App-ஐத் திறந்து `Allow microphone` அழுத்தவும்.
2. நம்பகமான மூலத்திலிருந்து multilingual `ggml-base.bin` வாங்க/பதிவிறக்கி, `Import multilingual Whisper .bin model` மூலம் நிறுவவும். `.en.bin` பயன்படுத்த வேண்டாம்.
3. `Enable keyboard` மற்றும் `Choose keyboard` தேர்வு செய்யவும்.
4. Test Editor-ல் cursor வைத்து Keyboard-ஐத் தேர்வு செய்யவும்.
5. Airplane Mode ON; Wi‑Fi மற்றும் Mobile Data OFF.
6. Record → `இன்று நான் தமிழில் பேசுகிறேன்` → Stop. Tamil output தோன்றுகிறதா பார்க்கவும்.
7. English மற்றும் mixed-language, 25s limit, empty audio, password field, input focus change ஆகியவற்றையும் சோதிக்கவும்.
8. Speech detection accuracy, device RAM, battery, speed ஆகியவற்றைப் பதிவு செய்யவும்.

**இங்கு Android SDK/Gradle இல்லாததால் முழுமையான APK Build/Phone Test முடிக்கப்படவில்லை.** JVM/static check வெற்றி = APK success அல்ல.


புதிய வழிமுறைகள்: `GITHUB_APK_BUILD_TA.md`. `main` branch-ல் push செய்ததும் Actions தானாக build முயற்சிக்கும்.
