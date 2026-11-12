#!/usr/bin/env bash
# Dò công cụ phát triển trên máy (Windows Git Bash / macOS / Linux) và ghi ra .dev-env (không commit).
# Chạy một lần trên mỗi máy, chạy lại khi cài thêm công cụ:   bash tools/dev_env.sh
# Dùng:   source .dev-env   → có JAVA_HOME, ANDROID_SDK, ADB, DEXDUMP, JADX, PYTHON, MSYS_NO_PATHCONV
cd "$(dirname "$0")/.." || exit 1
OUT=.dev-env
missing=()

first_dir() { for d in "$@"; do [ -n "$d" ] && [ -d "$d" ] && { echo "$d"; return; }; done; }
first_file() { for f in "$@"; do [ -n "$f" ] && [ -f "$f" ] && { echo "$f"; return; }; done; }

# JDK: JAVA_HOME có sẵn → JDK đi kèm Android Studio.
JAVA_HOME_FOUND=$(first_dir "$JAVA_HOME" \
  "/c/Program Files/Android/Android Studio/jbr" \
  "$LOCALAPPDATA/Programs/Android Studio/jbr" \
  "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  "$HOME/android-studio/jbr" "/opt/android-studio/jbr")
[ -z "$JAVA_HOME_FOUND" ] && missing+=("JDK (cài Android Studio)")

# Android SDK: biến môi trường → local.properties → vị trí mặc định.
SDK_FROM_PROPS=$(grep -s '^sdk.dir=' local.properties | cut -d= -f2- | sed 's/\:/:/g; s/\\/\//g')
[ -n "$SDK_FROM_PROPS" ] && command -v cygpath >/dev/null && SDK_FROM_PROPS=$(cygpath -u "$SDK_FROM_PROPS")
SDK=$(first_dir "$ANDROID_HOME" "$ANDROID_SDK_ROOT" "$SDK_FROM_PROPS" "$LOCALAPPDATA/Android/Sdk" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk")
[ -z "$SDK" ] && missing+=("Android SDK (mở Android Studio → SDK Manager)")

EXE=""; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) EXE=".exe";; esac
ADB=$(first_file "$SDK/platform-tools/adb$EXE"); [ -z "$ADB" ] && missing+=("adb (SDK Manager → Platform-Tools)")
BT=$(ls -d "$SDK"/build-tools/*/ 2>/dev/null | sort -V | tail -1)
DEXDUMP=$(first_file "${BT}dexdump$EXE"); [ -z "$DEXDUMP" ] && missing+=("dexdump (SDK Manager → Build-Tools)")

# JADX: trong PATH → thư mục Downloads / tools.
JADX=$(command -v jadx-gui || command -v jadx)
[ -z "$JADX" ] && JADX=$(ls "$HOME"/Downloads/jadx*/jadx-gui*.exe "$HOME"/Downloads/jadx*/bin/jadx-gui "$HOME"/tools/jadx*/bin/jadx-gui 2>/dev/null | head -1)
[ -z "$JADX" ] && missing+=("JADX (github.com/skylot/jadx/releases, bản with-jre)")

# Bản python "giả" của Microsoft Store không chạy được: chỉ nhận bản trả về version thật.
PYTHON=""; for p in python3 python; do c=$(command -v $p) && "$c" --version 2>&1 | grep -q '^Python 3' && { PYTHON=$c; break; }; done
[ -z "$PYTHON" ] && missing+=("Python 3 (python.org)")

cat > "$OUT" <<ENV
# Sinh bởi tools/dev_env.sh ngày $(date +%F) - không commit. Chạy lại script khi đổi máy / cài thêm công cụ.
export MSYS_NO_PATHCONV=1
export JAVA_HOME="$JAVA_HOME_FOUND"
export ANDROID_SDK="$SDK"
export ANDROID_HOME="$SDK"   # Gradle tìm SDK khi chưa có local.properties
export ADB="$ADB"
export DEXDUMP="$DEXDUMP"
export JADX="$JADX"
export PYTHON="$PYTHON"
ENV

echo "Đã ghi $OUT:"; grep '^export' "$OUT" | sed 's/^export /  /'
# Thiết bị đang cắm
[ -n "$ADB" ] && { echo "Thiết bị:"; "$ADB" devices | tail -n +2 | sed '/^$/d; s/^/  /'; }
if [ ${#missing[@]} -gt 0 ]; then
  echo "THIẾU:"; printf '  - %s\n' "${missing[@]}"; exit 1
fi
echo "OK: đủ công cụ."
