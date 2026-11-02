# ARCHITECTURE
- UI: `MainScreen.kt` (Compose) + `MainActivity.kt` (الصلاحيات، فتح الملفات، Shizuku).
- الحالة: `MainViewModel` يعرض `StateFlow<UiState>`.
- البيانات: `LocalFiles` (java.io.File على IO dispatcher) و`ShizukuHelper` + `FileService` (UserService عبر AIDL `IFileService`).
- التوقيع: متغيرات بيئة KEYSTORE_PATH / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD تقرؤها app/build.gradle.kts.
