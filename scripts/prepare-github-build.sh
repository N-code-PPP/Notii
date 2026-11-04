#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "==> Preparing Notii build environment and signing keys..."

# 1. Prepare .env from .env.example if missing
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

# 2. Restore debug.keystore from debug.keystore.base64 if not already present
if [ ! -f "debug.keystore" ] && [ -f "debug.keystore.base64" ]; then
  tr -d '[:space:]' < "debug.keystore.base64" | base64 -d > "debug.keystore"
  echo "  [OK] Materialized debug.keystore from debug.keystore.base64"
fi

# 3. Restore my-upload-key.jks from KEYSTORE_BASE64_SECRET or my-upload-key.jks.base64
if [ -n "${KEYSTORE_BASE64_SECRET:-}" ]; then
  printf "%s" "$KEYSTORE_BASE64_SECRET" | tr -d '[:space:]' | base64 -d > "my-upload-key.jks"
  echo "  [OK] Materialized my-upload-key.jks from KEYSTORE_BASE64 secret"
elif [ ! -f "my-upload-key.jks" ] && [ -f "my-upload-key.jks.base64" ]; then
  tr -d '[:space:]' < "my-upload-key.jks.base64" | base64 -d > "my-upload-key.jks"
  echo "  [OK] Materialized my-upload-key.jks from my-upload-key.jks.base64"
fi

echo "==> Environment and signing keys are ready."
