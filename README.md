# FS Manager by F-Studio

تطبيق Android Native لإدارة الملفات (Kotlin + Jetpack Compose + تكامل Shizuku الرسمي).
الحزمة: `com.fstudio.fsmanager` — minSdk 26 / targetSdk 34.

## البناء عبر GitHub Actions (من الهاتف)
1. أنشئ مستودعاً جديداً على GitHub وارفع `project.zip` إلى الجذر.
2. أنشئ الملف `.github/workflows/android.yml` من المحرر في الموقع والصق محتوى `android.yml`.
3. Actions ← FS Manager CI ← Run workflow ← `unpack` (يفك الضغط ويرفع الملفات).
4. شغّل `keystore` مرة واحدة فقط، نزّل Artifact باسم keystore-and-secrets، وأنشئ 4 Secrets
   من ملف secrets.txt: KEYSTORE_BASE64 و KEYSTORE_PASSWORD و KEY_ALIAS و KEY_PASSWORD. احتفظ بنسخة من ملف .jks في مكان آمن.
5. شغّل `release` فتحصل على APK موقّع وملف AAB في Artifacts. شغّل `debug` لنسخة تجريبية.

## مهم
- لا تغيّر المفتاح بعد أول نشر؛ بدونه لن تستطيع تحديث التطبيق.
- versionCode يزيد تلقائياً مع كل تشغيل (GITHUB_RUN_NUMBER). غيّر versionName يدوياً في app/build.gradle.kts.
