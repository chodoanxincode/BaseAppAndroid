#!/usr/bin/env bash
# Dump cây view màn đang mở (uiautomator) và báo chữ / view tràn khỏi màn hoặc khỏi view cha.
# Dùng: ui_dump.sh [tên_file_lưu.xml]
source "$(dirname "$0")/env.sh"; OUT="${1:-ui_dump.xml}"
"$ADB" shell uiautomator dump /sdcard/ui_dump.xml >/dev/null && "$ADB" pull /sdcard/ui_dump.xml "$OUT" >/dev/null && "$ADB" shell rm /sdcard/ui_dump.xml
python -I "$(dirname "$0")/ui_overflow.py" "$OUT"
