# Notii — Android App

Notii is a Kotlin + Jetpack Compose note-taking and document/photo capture application backed by Firebase Authentication (Google Sign-In via Credential Manager) and Cloud Firestore with offline-first local persistence and multi-device sync deduplication.

---

## Building from GitHub (CI & Local)

This repository is pre-configured with the Gradle Wrapper (`9.3.1`), compilation signing keys, and a GitHub Actions workflow (`.github/workflows/android-build.yml`) so it compiles out of the box when pushed to GitHub or cloned locally.

### 1. Automatic GitHub Actions Compilation
Every push or pull request to `main` / `master` (or manual run via **Actions -> Android CI Build & Sign -> Run workflow**) automatically:
1. Sets up **JDK 21** and **Android SDK Platform 36**.
2. Prepares `.env` from `.env.example` and restores `debug.keystore` and `my-upload-key.jks`.
3. Runs unit tests (`./gradlew :app:testDebugUnitTest`).
4. Builds signed **Debug APK** (`app-debug.apk`), **Release APK** (`app-release.apk`), and **Release App Bundle** (`app-release.aab`).
5. Uploads the compiled APKs and AAB as downloadable workflow artifacts.

---

## Compilation Signing Keys

Both Debug and Release signing configurations are pre-configured in `app/build.gradle.kts` and share the project certificate registered in `app/google-services.json` so Google Sign-In and Firebase Firestore work seamlessly across both build variants:

- **Certificate Fingerprints (Registered in Firebase `app/google-services.json`)**:
  - **SHA-1**: `F4:1C:DB:AF:11:76:9C:36:DF:AC:DA:15:05:85:79:FA:B2:EA:91:53`
  - **SHA-256**: `64:2F:44:93:3C:A7:75:2F:01:EA:BE:57:DD:9A:1E:1F:B0:AE:AB:E3:82:DE:12:55:36:77:F6:AD:BD:1C:18:79`

| Build Type | Keystore File | Base64 Backup | Alias | Default Store / Key Password |
| :--- | :--- | :--- | :--- | :--- |
| **Debug** (`debugConfig`) | `debug.keystore` | `debug.keystore.base64` | `androiddebugkey` | `android` / `android` |
| **Release** (`release`) | `my-upload-key.jks` | `my-upload-key.jks.base64` | `upload` | `STORE_PASSWORD` / `KEY_PASSWORD` (defaults to `android` in CI) |

> **Note:** On a fresh `git clone`, the root `build.gradle.kts` (or `./scripts/prepare-github-build.sh`) automatically:
> - Decodes `debug.keystore.base64` -> `debug.keystore`
> - Decodes `my-upload-key.jks.base64` -> `my-upload-key.jks`
> - Decodes `app/google-services.json.base64` -> `app/google-services.json`
> - Copies `.env.example` -> `.env`

### Optional GitHub Actions Secrets (Custom Configuration)
The workflow works with **zero configuration** out of the box using the included signing key and Firebase configuration. If you wish to override values via GitHub Repository Secrets (**Settings -> Secrets and variables -> Actions**):
- `GOOGLE_SERVICES_JSON`: Optional base64-encoded `google-services.json` to override Firebase project configuration.
- `KEYSTORE_BASE64`: Base64-encoded `.jks` keystore containing key alias `upload` (defaults to `my-upload-key.jks.base64`).
- `STORE_PASSWORD`: Keystore password (defaults to `android`).
- `KEY_PASSWORD`: Key password for alias `upload` (defaults to `android`).
- `GOOGLE_WEB_CLIENT_ID`: Optional override for `GOOGLE_WEB_CLIENT_ID` in `.env`.

---

## Local Build Commands

### Build Signed Debug APK
```bash
./gradlew :app:assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Build Signed Release APK & Bundle
```bash
STORE_PASSWORD=android KEY_PASSWORD=android ./gradlew :app:assembleRelease :app:bundleRelease
```
Outputs:
- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`
