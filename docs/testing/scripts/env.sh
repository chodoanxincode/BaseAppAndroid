#!/usr/bin/env bash
# Dùng chung cho các script test: lấy đường dẫn công cụ từ .dev-env (tools/dev_env.sh), không có thì tự dò adb.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
[ -f "$ROOT/.dev-env" ] || bash "$ROOT/tools/dev_env.sh" >/dev/null
source "$ROOT/.dev-env"
export MSYS_NO_PATHCONV=1
[ -z "$ADB" ] && command -v adb >/dev/null && ADB=adb
[ -z "$ADB" ] && { echo "Không tìm thấy adb - chạy: bash tools/dev_env.sh" >&2; exit 1; }
PKG="${PKG:-$(grep -m1 'applicationId' "$ROOT/app/build.gradle.kts" | sed -E 's/.*"(.*)".*/\1/')}"
