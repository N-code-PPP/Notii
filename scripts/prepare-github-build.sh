#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "==> Preparing Notii build environment and signing keys..."

# 1. Ensure Android SDK licenses are populated if ANDROID_HOME or ANDROID_SDK_ROOT is set
SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -n "$SDK_DIR" ] && [ -d "$SDK_DIR" ] && [ -w "$SDK_DIR" ]; then
  mkdir -p "$SDK_DIR/licenses"
  printf "\n8933bad161af4178b1185d1a37fbf41ea5269c55\nd56f5187479451eabf01fb78af6dfcb131a6481e\n24333f8a63b6825ea9c5514f83c2829b004d1fee\n" > "$SDK_DIR/licenses/android-sdk-license"
  printf "\n84831b9409646a918e30573bab4c9c91346d8abd\n504667f4c0de7af1a06de9f4b1727b84351f2910\n" > "$SDK_DIR/licenses/android-sdk-preview-license"
  printf "\n601085b94cd77f0b54ff86406957099ebe79c4d6\n" > "$SDK_DIR/licenses/android-googletv-license"
  printf "\nceff83576aac4f7f37cb98fe189e9fb3c49d3b81\n" > "$SDK_DIR/licenses/android-googlexr-license"
  printf "\n859f317696f67ef3d7f30a50a5560e7834b43903\n" > "$SDK_DIR/licenses/android-sdk-arm-dbt-license"
  printf "\n33b6a2b64607f11b759f320ef9dff4ae5c47d97a\n" > "$SDK_DIR/licenses/google-gdk-license"
  printf "\n3a7848b1d7366ca5cfa0be477627644ca73f47e3\n" > "$SDK_DIR/licenses/microxr-sysimage-license"
  printf "\ne9acab5b5fbb560a72cfaecce8946896ff6aab9d\n" > "$SDK_DIR/licenses/mips-android-sysimage-license"
  echo "  [OK] Pre-populated Android SDK licenses in $SDK_DIR/licenses"
fi

# 2. Prepare .env from .env.example if missing
if [ ! -f ".env" ] && [ -f ".env.example" ]; then
  cp ".env.example" ".env"
  echo "  [OK] Created .env from .env.example"
fi

if [ -n "${GOOGLE_WEB_CLIENT_ID_SECRET:-}" ]; then
  grep -v '^GOOGLE_WEB_CLIENT_ID=' .env > .env.tmp || true
  echo "GOOGLE_WEB_CLIENT_ID=${GOOGLE_WEB_CLIENT_ID_SECRET}" >> .env.tmp
  mv .env.tmp .env
  echo "  [OK] Injected GOOGLE_WEB_CLIENT_ID from environment secret"
fi

# 3. Restore my-upload-key.jks from KEYSTORE_BASE64_SECRET or my-upload-key.jks.base64 first
if [ -n "${KEYSTORE_BASE64_SECRET:-}" ]; then
  printf "%s" "$KEYSTORE_BASE64_SECRET" | tr -d '[:space:]' | base64 -d > "my-upload-key.jks"
  echo "  [OK] Materialized my-upload-key.jks from KEYSTORE_BASE64 secret"
elif [ ! -f "my-upload-key.jks" ] && [ -f "my-upload-key.jks.base64" ]; then
  tr -d '[:space:]' < "my-upload-key.jks.base64" | base64 -d > "my-upload-key.jks"
  echo "  [OK] Materialized my-upload-key.jks from my-upload-key.jks.base64"
fi

# 4. Restore debug.keystore from debug.keystore.base64, signing-key-debug.b64, or my-upload-key.jks
if [ ! -f "debug.keystore" ] && [ -f "debug.keystore.base64" ]; then
  tr -d '[:space:]' < "debug.keystore.base64" | base64 -d > "debug.keystore" || true
  echo "  [OK] Materialized debug.keystore from debug.keystore.base64"
fi

if [ ! -f "debug.keystore" ] && [ -f "signing-key-debug.b64" ]; then
  tr -d '[:space:]' < "signing-key-debug.b64" | base64 -d > "debug.keystore" || true
  echo "  [OK] Materialized debug.keystore from signing-key-debug.b64"
fi

if [ ! -f "debug.keystore" ]; then
  if [ -f "my-upload-key.jks" ]; then
    echo "  [INFO] Deriving debug.keystore from my-upload-key.jks..."
    keytool -importkeystore -srckeystore "my-upload-key.jks" -srcstorepass android -srckeypass android -srcalias upload -destkeystore "debug.keystore" -deststorepass android -destkeypass android -destalias androiddebugkey -noprompt || true
  fi
  if [ ! -f "debug.keystore" ]; then
    echo "  [INFO] Generating fallback debug.keystore with keytool..."
    keytool -genkeypair -v -keystore "debug.keystore" -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" || true
  fi
fi

# 5. Restore app/google-services.json from GOOGLE_SERVICES_JSON_SECRET or app/google-services.json.base64
if [ -n "${GOOGLE_SERVICES_JSON_SECRET:-}" ]; then
  printf "%s" "$GOOGLE_SERVICES_JSON_SECRET" | tr -d '[:space:]' | base64 -d > "app/google-services.json"
  echo "  [OK] Materialized app/google-services.json from GOOGLE_SERVICES_JSON secret"
elif [ ! -f "app/google-services.json" ] && [ -f "app/google-services.json.base64" ]; then
  tr -d '[:space:]' < "app/google-services.json.base64" | base64 -d > "app/google-services.json"
  echo "  [OK] Materialized app/google-services.json from app/google-services.json.base64"
fi

echo "==> Environment and signing keys are ready."
