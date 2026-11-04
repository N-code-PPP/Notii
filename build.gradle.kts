import java.util.Base64

// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Automatically materialize signing keystores from their committed .base64 / .b64 files
// on fresh GitHub clones or CI runners when the binary keystore files are not yet present.
fun ensureKeystoreFromBase64(keystoreFile: File, vararg base64Candidates: File) {
  if (keystoreFile.exists()) return
  val sourceFile = base64Candidates.firstOrNull { it.exists() } ?: return
  val cleaned = sourceFile.readText().replace("\\s".toRegex(), "")
  if (cleaned.isNotEmpty()) {
    keystoreFile.writeBytes(Base64.getDecoder().decode(cleaned))
  }
}

ensureKeystoreFromBase64(
  keystoreFile = file("debug.keystore"),
  file("debug.keystore.base64"),
  file("signing-key-debug.b64")
)
ensureKeystoreFromBase64(
  keystoreFile = file("my-upload-key.jks"),
  file("my-upload-key.jks.base64")
)

val googleServicesFile = file("app/google-services.json")
val googleServicesBase64File = file("app/google-services.json.base64")
if (!googleServicesFile.exists() && googleServicesBase64File.exists()) {
  val cleaned = googleServicesBase64File.readText().replace("\\s".toRegex(), "")
  googleServicesFile.writeBytes(Base64.getDecoder().decode(cleaned))
}

val envFile = file(".env")
val envExampleFile = file(".env.example")
if (!envFile.exists() && envExampleFile.exists()) {
  envExampleFile.copyTo(envFile, overwrite = false)
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}
