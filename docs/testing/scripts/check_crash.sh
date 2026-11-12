#!/usr/bin/env bash
# Tìm crash / ANR / exception của app kể từ lần `adb logcat -c` gần nhất. Chỉ lấy log của app (không lẫn app khác).
# Dùng: check_crash.sh [package]   → "OK: ..." hoặc in các dòng lỗi (exit 1).
# Log đã biết là vô hại: thêm regex vào IGNORE (biến môi trường, hoặc sửa mặc định dưới) và ghi lý do trong test plan.
IGNORE="${IGNORE:-AbtException|FirebaseABTesting}"   # Remote Config cảnh báo khi app không có Analytics SDK
source "$(dirname "$0")/env.sh"; PKG="${1:-$PKG}"
# 1) Crash / ANR: buffer crash + system, lọc theo tên package.
CRASH=$("$ADB" logcat -d -b crash,system -v time 2>/dev/null | grep -E "FATAL EXCEPTION|ANR in $PKG|Process: $PKG" -A12 | grep -B2 -A10 "$PKG")
# 2) Exception / StrictMode trong log của process app đang chạy.
PID=$("$ADB" shell pidof "$PKG" | tr -d '\r')
OWN=""
[ -n "$PID" ] && OWN=$("$ADB" logcat -d -v time --pid="$PID" 2>/dev/null | grep -E "Exception|StrictMode|E/AndroidRuntime" | grep -v "AdbDriver" | grep -Ev "$IGNORE")
OUT=$(printf '%s\n%s' "$CRASH" "$OWN" | sed '/^$/d')
if [ -z "$OUT" ]; then echo "OK: không có crash / ANR / exception của $PKG"; else echo "$OUT" | head -80; exit 1; fi
