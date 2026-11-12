#!/usr/bin/env bash
# Ép GC rồi in số object của app (Activities / Views / ViewRootImpl / AppContexts) và PSS.
# Dùng: leak_check.sh <nhãn> [package]  - chạy trước và sau một vòng mở/đóng màn để so.
# Cần bản debuggable (run-as). Không đổi cài đặt máy.
source "$(dirname "$0")/env.sh"; LABEL="${1:-snapshot}"; PKG="${2:-$PKG}"
PID=$("$ADB" shell pidof "$PKG" | tr -d '\r')
[ -z "$PID" ] && { echo "$LABEL: app không chạy"; exit 1; }
for i in 1 2 3; do "$ADB" shell run-as "$PKG" kill -10 "$PID" 2>/dev/null; sleep 1; done   # SIGUSR1 = ART ép GC
M=$("$ADB" shell dumpsys meminfo "$PKG" | tr -d '\r')
pick() { echo "$M" | grep -oE "$1: *[0-9]+" | head -1 | grep -oE "[0-9]+$"; }
echo "$LABEL | Activities=$(pick Activities) Views=$(pick ' Views') ViewRootImpl=$(pick ViewRootImpl) AppContexts=$(pick AppContexts) PSS_KB=$(echo "$M" | grep -E '^ *TOTAL PSS:|^ *TOTAL ' | head -1 | grep -oE '[0-9]+' | head -1)"
