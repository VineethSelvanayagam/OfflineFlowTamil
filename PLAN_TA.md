# Offline Flow Tamil — நிலை மற்றும் அடுத்த கட்டங்கள்

- v0.1—v0.5: IME, AudioRecord, offline Whisper JNI, file import, local test Editor, data validation ஆகியவற்றின் source prototype.
- **v0.7:** native speech engine கட்டாயமாக்கப்பட்டது; Engine இல்லாத APK வெற்றி போல வராது. Native library APK-இல் உள்ளதா GitHub Actions சோதனை; English-only model நிராகரிப்பு.
- அடுத்த கட்டம்: Android Studio / GitHub Actions மூலம் **உண்மையான APK Build வெற்றி** மற்றும் Airplane Mode-ல் Tamil Voice Recognition சோதனை.
- அதன் பிறகு: speed, mixed Tamil-English accuracy, partial dictation/streaming மற்றும் offline text correction.

**Status:** source checks முடியும்; real APK / device tests நிலுவை.
