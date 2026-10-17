from pathlib import Path
from xml.etree import ElementTree as ET
import struct

root = Path(__file__).resolve().parents[1]
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
android = '{http://schemas.android.com/apk/res/android}'
permissions = [el.attrib.get(android+'name') for el in manifest.findall('uses-permission')]
assert 'android.permission.INTERNET' not in permissions, permissions
assert 'android.permission.RECORD_AUDIO' in permissions, permissions
service = manifest.find('application/service')
assert service is not None
assert service.attrib.get(android+'permission') == 'android.permission.BIND_INPUT_METHOD'

sources = (root / 'app/src/main/java/com/offlineflow/tamil')
ime = (sources / 'VoiceImeService.kt').read_text()
audio = (sources / 'AudioCapture.kt').read_text()
bridge = (sources / 'WhisperBridge.kt').read_text()
activity = (sources / 'MainActivity.kt').read_text()
assert 'override fun onStartInput(' in ime
assert 'override fun onFinishInput()' in ime
assert 'originGeneration == generation' in ime
assert 'InputType.TYPE_TEXT_VARIATION_PASSWORD' in ime
assert 'MAX_SECONDS = 25' in audio
assert '@Synchronized fun replaceModel' in bridge
assert 'DataInputStream(FileInputStream(temporary))' in activity
assert struct.pack('<I', 0x67676d6c) == bytes([0x6c, 0x6d, 0x67, 0x67])
assert (root / '.github/workflows/android-debug.yml').is_file()
assert (sources / 'AudioValidation.kt').is_file()
assert 'AudioValidation.evaluate(pcm)' in ime
assert 'operationSerial' in ime
assert 'Offline voice-typing test area' in activity
assert 'EditText(this)' in activity
assert 'versionName = "0.7.0"' in (root/'app/build.gradle.kts').read_text()
assert 'ndkVersion = "26.3.11579264"' in (root/'app/build.gradle.kts').read_text()
native = (root / 'app/src/main/cpp/whisper_jni.cpp').read_text()
assert 'nativeTranscribeUtf8' in bridge and 'nativeTranscribeUtf8' in native
assert 'NewByteArray' in native and 'SetByteArrayRegion' in native
assert 'NewStringUTF(text.c_str())' not in native
assert 'Charsets.UTF_8' in bridge
assert 'Free device RAM now:' in activity
workflow = (root / '.github/workflows/android-debug.yml').read_text()
assert 'aapt" dump permissions' in workflow
assert 'android.permission.INTERNET' in workflow
# Build safety: the previous shell-only APK must no longer be buildable.
app_gradle = (root / 'app/build.gradle.kts').read_text()
assert 'externalNativeBuild {' in app_gradle
assert 'if (enableWhisper)' not in app_gradle
assert 'enableWhisper=false' not in (root / 'gradle.properties').read_text()
assert 'if (context && !whisper_is_multilingual(context))' in native
assert 'if (!raw) return 0;' in native
assert 'if (!raw) return makeUtf8Bytes(env, "");' in native
assert 'Confirm APK contains Whisper JNI and no INTERNET permission' in workflow
assert 'lib/arm64-v8a/libofflinewhisper.so' in workflow
assert '-PenableWhisper=true' not in workflow
assert 'v0.7' in (root / 'README.md').read_text() if (root / 'README.md').exists() else True
print('PASS: source/privacy/model/JNI + 9 fail-closed build assertions')

assert 'push:' in workflow and 'workflow_dispatch:' in workflow
assert 'OfflineFlowTamil-v0.7-UNTESTED-debug-apk' in workflow
assert 'candidateContext = nativeLoad(temporary.absolutePath)' in bridge
assert (root/'GITHUB_APK_BUILD_TA.md').exists()
print('PASS: GitHub trigger, package, and model preflight checks')
