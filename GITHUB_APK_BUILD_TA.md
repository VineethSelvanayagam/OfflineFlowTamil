# Offline Flow Tamil v0.7 — GitHub Actions APK Build வழிகாட்டி

## என்ன தயாராகியுள்ளது?
Android Kotlin/NDK/CMake project + GitHub Actions தானியங்கு Build வேலைப்பாடு. இது இன்னும் **APK வெற்றிபெற்றது** என்று அர்த்தமல்ல. CI இயங்கிய பிறகே வெற்றி அல்லது பிழையை உறுதிப்படுத்த முடியும்.

## GitHub-ல் தனி Repository உருவாக்குதல்

1. https://github.com/new சென்று `OfflineFlowTamil` என்று புதிய **Private** repository உருவாக்குங்கள். `README`/`.gitignore` தானாக உருவாக்காமல் Empty repository ஆக வைக்கலாம்.
2. இங்கு கொடுக்கப்பட்ட ZIP-ஐ extract செய்து `OfflineFlowTamil_v0.7` என்ற folder-ஐத் திறக்கவும்.
3. அந்த folder **உள்ளே உள்ள அனைத்தையும்**, குறிப்பாக `.github` என்ற hidden folder-ஐ, புதிய Repository-யின் **root**-க்கு upload செய்யுங்கள். `OfflineFlowTamil_v0.7` என்ற ஒரு கூடுதல் parent folder இருக்கக் கூடாது.
4. கணினியில் `git` மூலம் upload செய்வது மிகவும் நம்பகமானது (கீழே உள்ள commands).
5. `main` branch-ல் commit/push செய்தவுடன் Actions தானாக Build முயற்சிக்கும். இல்லையெனில் Repository → Actions → **Android Offline Flow Tamil — Debug APK** → **Run workflow**.
6. Build வெற்றி பெற்றால் Actions run → Artifacts → `OfflineFlowTamil-v0.7-UNTESTED-debug-apk`-ஐப் பதிவிறக்குங்கள். ZIP-ஐ extract செய்தால் APK கிடைக்கும்.

## Git மூலம் Source Upload (புதிய காலியான Repository)

```sh
cd OfflineFlowTamil_v0.7
git init -b main
git add -A
git commit -m 'Offline Flow Tamil Android v0.7'
git remote add origin https://github.com/YOUR_USERNAME/OfflineFlowTamil.git
git push -u origin main
```

`YOUR_USERNAME`-ஐ உங்கள் GitHub account login-ஆக மாற்றவும். GitHub credentials தேவைப்படலாம்; password-ஐ code கோப்புகளுக்குள் சேர்க்க வேண்டாம்.

## Build செய்யும்போது இணையம் எங்கே பயன்படுகிறது?
- GitHub runner: Android SDK, NDK, Gradle மற்றும் whisper.cpp engine-ஐ இணையத்திலிருந்து பெறும்.
- Android app: `INTERNET` permission இல்லை. இது microphone மற்றும் உள்ளூர் model மூலம் மட்டுமே இயங்கும்.
- Whisper multilingual model APK-க்குள் சேர்க்கப்படவில்லை. இதை பயனர் ஒருமுறை Download செய்து Android system file picker மூலம் Import செய்ய வேண்டும்.

## Model பெறுதல்

https://huggingface.co/ggerganov/whisper.cpp/tree/main → `ggml-base.bin` **multilingual** model பெறலாம். `ggml-base.en.bin` வேண்டாம்.

## மொபைலில் சரிபார்க்க வேண்டியவை
- `RECORD_AUDIO` அனுமதி மட்டும்; இணைய அனுமதி இல்லாமை
- Import செய்த model திறந்து வேலைசெய்யுமா
- Airplane mode-ல் தமிழ் மற்றும் ஆங்கில எழுத்தாக்கம்
- Password input-ல் microphone disabled
- Focus மாற்றும்போது பழைய transcribe முடிவு வேறு இடத்தில் சேராமல் இருக்கிறதா
- Low RAM மற்றும் பெரிய model import தோல்வி பாதுகாப்பாக கையாளப்படுகிறதா

## முக்கிய வரம்பு
CI compile success என்பது **Voice Typing சரியாக இயங்குகிறது** என்பதற்கான ஆதாரம் இல்லை. இறுதி தரத்தை உண்மையான ARM64 Android மொபைலில் சோதிக்க வேண்டும்.
