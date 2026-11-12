# Form test tính năng (dùng chung cho mọi app Android)

Thư mục `docs/testing/` gồm:

| File | Vai trò |
|---|---|
| `TEST_FORM.md` (file này) | Quy trình, cách rút ca test từ code, mã nhóm, checklist chung, cách đo, tiêu chí đạt, mẫu báo cáo. **Không sửa theo app.** |
| `<tinh-nang>.md` | Test plan của một tính năng, viết theo mẫu ở mục 10. Ví dụ đầy đủ: `examples/drum.md` (Drum của T04 Beat Maker). |
| `scripts/` | Script adb dùng chung: `check_crash.sh`, `leak_check.sh`, `ui_dump.sh` (+ `ui_overflow.py`), `env.sh`. |

Mang sang app khác: copy cả thư mục `docs/testing/`. Nếu khác package thì sửa `PKG` trong `scripts/env.sh` hoặc truyền
package qua tham số. Sau đó viết plan mới cho từng tính năng theo mục 2 và mục 10.

---

## 1. Khi nào test và khi nào coi là xong

**Bắt đầu test khi:** build debug cài được, unit test pass, tính năng đã đủ luồng chính (không còn TODO chặn luồng).

**Xong lượt test (exit criteria):**
- Mọi ca **P0** đạt. Ca **P1** đạt từ 90% trở lên, ca chưa đạt phải có lỗi được ghi lại.
- Không còn lỗi **S1 / S2** đang mở.
- Nhóm **M** (leak) và **C** (crash) đã chạy hết trên máy thật. `check_crash.sh` sạch sau cả lượt.
- Đã ghi **Báo cáo lượt test** (mục 9) ở cuối file plan.

**Chạy lại (regression)** sau mỗi lần sửa:
- các ca bị lỗi;
- các ca cùng nhóm có đụng tới file vừa sửa;
- **bộ smoke** của plan (các ca P0);
- `check_crash.sh`.

---

## 2. Rút ca test từ code (làm trước khi viết plan)

Ca test phải xuất phát từ code thật, không viết theo trí nhớ. Đọc code tính năng rồi lập lần lượt các bảng dưới đây
(đặt ở đầu file plan, mục "Phân tích"). **Mỗi dòng của mỗi bảng phải trỏ tới ít nhất một ID ca test.**

| # | Bảng | Lấy từ đâu trong code | Sinh ra ca nhóm |
|---|---|---|---|
| 2.1 | **Sơ đồ màn**: màn → màn, kiểu mở (start, result, CLEAR_TOP, dialog) | `startActivity`, `registerForActivityResult`, `Intent` flags, manifest (`screenOrientation`, `launchMode`, `exported`) | F, L, C |
| 2.2 | **Intent / sự kiện người dùng**: mọi `Intent` của Contract, mọi listener của view | `*Contract.kt`, `initListeners()` | F (mỗi Intent ≥ 1 ca), C (bấm dồn) |
| 2.3 | **Effect / kết quả thấy được**: toast, mở màn, animation, hộp thoại | `sealed interface *Effect`, `RecordEffect`… | F (mỗi Effect phải được quan sát thấy ít nhất một lần) |
| 2.4 | **State và cách vẽ**: mỗi trường state, mỗi nhánh `isVisible` / `isEnabled` | `*UiState`, `render()` | F, U (mọi trạng thái hiển thị: loading / rỗng / lỗi / có dữ liệu) |
| 2.5 | **Tài nguyên sống lâu**: player, SoundPool, coroutine Job, listener, receiver, file mở, animation lặp | `init {}`, `attach`, `onCleared`, `release`, `postInvalidateOnAnimation` | M, L, P |
| 2.6 | **Dữ liệu lưu**: Room, SharedPreferences, file; khoá và giá trị mặc định | `data/`, `*Repository*` | D, C (dữ liệu hỏng) |
| 2.7 | **Đầu vào từ ngoài**: file người dùng chọn, quyền, mạng, intent extras, asset | `ContentResolver`, `Permission`, `getIntExtra` | C, Q, N |
| 2.8 | **Hằng số / ngưỡng / công thức**: điểm, thời gian, giới hạn | `const val`, `when` theo ngưỡng | F (giá trị **ngay dưới / bằng / ngay trên** ngưỡng), A |
| 2.9 | **Luồng song song**: hai tính năng có thể chạy cùng lúc (thu + phát, lesson + đổi màn…) | các cờ `isRecording`, `isActive` trong `handleIntent` | S |
| 2.10 | **Rủi ro**: chỗ code đáng ngờ (IO trên main thread, `lateinit`, `!!`, vẽ lặp, logic trùng lặp lệch nhau) | đọc code | ca bất kỳ, ghi rõ rủi ro nào được bắt |
| 2.11 | **Khác biệt so với spec / app mẫu / design** đã chốt | PROGRESS.md, spec | F (kiểm đúng quyết định đã chốt, không kiểm theo app mẫu) |

---

## 3. Mã nhóm ca test (cố định giữa các app)

| Mã | Nhóm | Câu hỏi cần trả lời |
|---|---|---|
| **A** | Tự động | Unit / instrumentation test có pass không? Logic thuần (công thức, parser, state machine) đã có test chưa? |
| **F** | Chức năng | Mỗi thao tác có cho ra đúng kết quả theo spec / quyết định đã chốt không? Giá trị ngưỡng có đúng không? |
| **S** | Trạng thái kết hợp | Hai tính năng chạy cùng lúc có xung đột, kẹt hay âm thanh chồng sai không? |
| **L** | Vòng đời / gián đoạn | Back, Home, khoá màn, xoay, màn con, process bị kill, đổi ngôn ngữ: có mất trạng thái, chạy ngầm hay treo không? |
| **C** | Crash / biên | Bấm dồn, dữ liệu hỏng / thiếu / rất lớn, file bị xoá, extras sai: có crash / ANR không? |
| **M** | Leak / tài nguyên | Mở / đóng N lần: Activity, View, PSS có trở lại mức cũ không? Player có được release, còn âm ngầm không? |
| **U** | Giao diện | Có cắt chữ, tràn, chồng view, bị tai thỏ / thanh hệ thống che không? Đủ các trạng thái loading / rỗng / lỗi chưa? Cả en và vi |
| **P** | Hiệu năng | Có giật khung, chạm có trễ, khởi động màn có chậm không? Có IO trên main thread, vẽ lặp khi rảnh không? |
| **D** | Dữ liệu | Dữ liệu có còn sau kill / cập nhật app không? Giá trị mặc định có đúng không? Dữ liệu hỏng thì có tự phục hồi không? |
| **Q** | Quyền | Cho phép / từ chối / từ chối hẳn / thu hồi lúc đang chạy; khác nhau theo API (≤ 28, 29–32, ≥ 33) |
| **N** | Mạng | Offline, mạng chậm, mất mạng giữa chừng, có lại mạng (chỉ khi tính năng dùng mạng) |
| **H** | Âm thanh / phần cứng | Audio focus (app khác phát nhạc, cuộc gọi), cắm / rút tai nghe, Bluetooth, nút âm lượng, chế độ im lặng, rung |
| **X** | Trợ năng | Mọi nút có `contentDescription`, vùng chạm ≥ 48dp, TalkBack đọc được các thành phần chính |
| **G** | Quảng cáo / Remote Config | Ads lỗi / không fill / tắt bằng config thì app vẫn đi tiếp (theo CLAUDE.md). Chỉ test khi tính năng có ads |
| **V** | Cài đặt / nâng cấp | Cài đè từ bản trước (migration Room, key prefs cũ), cài mới, xoá dữ liệu app |
| **T** | Tương thích | API thấp nhất (`minSdk`) / cao nhất, màn hẹp / rộng / tablet, có / không tai thỏ, ROM OEM (MIUI, ColorOS…) |

Plan không cần đủ 16 nhóm. Nhóm nào không áp dụng thì ghi một dòng `Không áp dụng: <lý do>`, không bỏ trống.

---

## 4. Checklist chung cho mọi màn

Mọi màn trong phạm vi đều chạy checklist này. Trong plan đặt thành **một bảng ma trận** (hàng = mục, cột = màn), mỗi ô
ghi KQ. Các mục trong checklist không cần viết lại thành ca riêng.

| Mã | Mục kiểm | Cách làm |
|---|---|---|
| K1 | Mở màn không crash, không trắng, không nháy | Mở từ đường vào chính |
| K2 | Back hệ thống đúng hành vi (đóng / hỏi xác nhận / dừng tác vụ) | Back |
| K3 | Nút quay về trên màn (Home / ←) đúng như Back | Bấm nút |
| K4 | Bấm Back nhanh 2 lần: không crash, không thoát lố | Back × 2 |
| K5 | Home rồi quay lại: trạng thái còn, âm / animation đúng | Home → Recent |
| K6 | Khoá màn rồi mở: như K5 | Nút nguồn |
| K7 | Process bị kill ở nền rồi mở lại: không crash, khôi phục hợp lý | `am kill` |
| K8 | Bấm dồn mọi nút mở màn / hộp thoại: chỉ mở 1 lần | 10 lần TAP nhanh |
| K9 | Đủ trạng thái loading / rỗng / lỗi / có dữ liệu (nếu có) | Tạo điều kiện |
| K10 | Không cắt chữ / tràn ở **en** | Chụp + `ui_dump.sh` |
| K11 | Không cắt chữ / tràn ở **vi** (chữ dài hơn) | Đổi ngôn ngữ trong app |
| K12 | Tai thỏ / thanh hệ thống không che nội dung, nút | Chụp màn |
| K13 | Mọi nút có contentDescription, vùng chạm ≥ 48dp | `ui_dump.sh` (xem `content-desc`, bounds) |
| K14 | Không leak sau 5 lần mở / đóng | `leak_check.sh` |
| K15 | `check_crash.sh` sạch sau khi chạy K1–K14 | Script |

---

## 5. Môi trường test

Ghi trong plan máy nào đã chạy, máy nào chưa (`⏭`). Tối thiểu: **một máy thật** và **API thấp nhất qua emulator** (nếu
có điều kiện).

| Trục | Giá trị nên phủ |
|---|---|
| Android API | `minSdk` (vd. 24) · 28 (quyền bộ nhớ cũ) · 30 · 33+ (quyền media mới) · target |
| Màn hình | hẹp (360dp) · thường · rộng / tablet; có / không tai thỏ |
| Ngôn ngữ | en · vi (+ ngôn ngữ chữ dài nhất app hỗ trợ) |
| ROM | AOSP / Pixel · MIUI · ColorOS / Realme |
| Bộ nhớ | đủ · gần đầy (< 500 MB) |

**Không đổi cài đặt hệ thống hoặc bảo mật của máy test** (CLAUDE.md), gồm `wm size`, `wm density`, font scale, "Don't
keep activities", Developer options. Cần cấu hình đó thì dùng emulator. Được phép trên máy test: cài / gỡ app đang test,
`pm grant/revoke` quyền của app đang test, `am kill`, `am force-stop`, xoá file tạm do mình đẩy lên.

---

## 6. Viết ca test

Bảng ca test của mỗi nhóm:

```markdown
| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| F-10 | P0 | Đổi Style | Style → chọn Minimal → Apply | Về Drum, ảnh đổi, toast "Style has been applied." | ADB | ✅ |
```

- **ID** = `<mã nhóm>-<số>`. Không đánh lại số; ca bỏ thì ghi `(bỏ)` và giữ số đó.
- **Ưu tiên**: `P0` luồng chính / mất dữ liệu / crash (thuộc bộ smoke) · `P1` chức năng phụ, biên quan trọng · `P2` hiếm
  gặp, thẩm mỹ.
- **Bước**: đủ cụ thể để người khác làm lại được, ghi rõ **điều kiện trước** nếu có ("đã chọn bài", "đang thu").
- **Kỳ vọng**: phải kiểm được (thấy, nghe, log, số liệu). Không viết "chạy đúng" chung chung.
- **Chạy**: `Unit` (test tự động) · `ADB` (lệnh / AdbDriver / script, làm lại được) · `Tay` (cần người: multi-touch, rung,
  tai nghe, nghe chất lượng âm).
- **KQ**: `✅` đạt · `❌ #n` lỗi (n = số trong bảng lỗi) · `⚠️` đạt nhưng có lưu ý · `⏭` chưa chạy (ghi lý do) · `—`
  không áp dụng.

---

## 7. Cách kiểm từng nhóm

Trên Windows Git Bash các script đã tự set `MSYS_NO_PATHCONV=1`. Lệnh adb lẻ thì cần tự `export MSYS_NO_PATHCONV=1`.

### Điều khiển máy
- Máy chặn `adb shell input` (MIUI…): dùng broadcast AdbDriver (bản debug): `TAP`, `TAPS` (chuỗi chạm có mốc ms),
  `SWIPE`, `TYPE`, `BACK`. Xem `app/src/debug/.../AdbDriver.kt`.
- Lấy toạ độ: chạy `scripts/ui_dump.sh` → lấy `bounds` theo `resource-id`.

### C: crash / ANR
- Đầu mỗi nhóm chạy `adb logcat -c`, cuối nhóm chạy `scripts/check_crash.sh` (chỉ lọc log của process app). Log đã
  biết là vô hại thì thêm regex vào `IGNORE` trong script và ghi lý do vào test plan.
- Bấm dồn: TAP liên tiếp không chờ, hoặc `TAPS` với mốc cách nhau 10–50 ms.
- Kill: Home → `adb shell am kill <pkg>` → mở lại từ Recent (hoặc `am start` activity gốc).
- Extras sai: `adb shell am start -n <pkg>/<activity> --ei <key> <giá trị lạ>` (chỉ với activity `exported`, hoặc qua
  luồng trong app).

### M: leak / tài nguyên
- `scripts/leak_check.sh trước` → làm N vòng mở / đóng → quay về màn gốc → `scripts/leak_check.sh sau`.
- **Đạt:** `Activities` = số màn đang mở; `Views` / `ViewRootImpl` không tăng theo N (so N = 5 với N = 10);
  PSS tăng < 5 MB.
- Âm ngầm: `adb shell dumpsys audio | grep -iE "player|state"`. Sau khi rời màn không còn player `started` của app.
- Có LeakCanary (debugImplementation) thì đọc thêm thông báo của nó. Base hiện chưa gắn.

### U: giao diện
- Chụp: `adb exec-out screencap -p > <file>.png`, rồi **mở ảnh xem bằng mắt**.
- `scripts/ui_dump.sh` báo view tràn khỏi cha hoặc khỏi màn. Script **không** bắt được chữ bị cắt *bên trong* một
  TextView (thiếu dòng, có "…"), nên phải xem ảnh.
- Đổi ngôn ngữ: Settings của app → Language.

### P: hiệu năng
- Khung hình: `dumpsys gfxinfo <pkg> reset` → thao tác 15 s → `dumpsys gfxinfo <pkg>`. Ghi `Janky frames`, p90, p99.
- Vẽ khi rảnh: reset → không chạm 5 s → `Total frames rendered` phải ≈ 0.
- Mở màn: `adb shell am start -W -n <pkg>/<activity>` (activity exported) hoặc đọc dòng `ActivityTaskManager: Displayed`
  trong logcat.
- CPU khi rảnh: `adb shell top -n 1 -p <pid>`.

### Q: quyền
- `adb shell pm revoke <pkg> <permission>` / `pm grant` (chỉ quyền của app đang test). Thu hồi quyền khi app đang chạy
  sẽ làm process bị kill, đó là hành vi đúng của Android: kiểm tra mở lại không crash.
- "Từ chối hẳn": từ chối 2 lần trong hộp thoại hệ thống.

### H: âm thanh / phần cứng
- Audio focus: mở app nhạc khác phát → quay lại app; hoặc gọi điện vào máy.
- Tai nghe: cắm / rút khi đang phát (ACTION_AUDIO_BECOMING_NOISY).
- Hầu hết là ca `Tay`.

### D / V: dữ liệu, nâng cấp
- Xem dữ liệu: `adb shell run-as <pkg> ls -la files shared_prefs databases`, `run-as <pkg> cat shared_prefs/<x>.xml`.
- Làm hỏng dữ liệu: `run-as <pkg>` sửa hoặc xoá file của chính app.
- Nâng cấp: cài bản cũ (checkout commit trước) → tạo dữ liệu → `adb install -r` bản mới.

---

## 8. Mức độ lỗi

| Mức | Nghĩa | Ví dụ |
|---|---|---|
| **S1** | Crash, ANR, mất dữ liệu, leak Activity | Back lúc đang lưu thì crash |
| **S2** | Sai chức năng chính, âm chạy ngầm, kẹt trạng thái, sai điểm | Rời màn nhạc vẫn phát; sao danh sách ≠ sao kết quả |
| **S3** | Sai UI rõ ràng (cắt chữ, che nút), thiếu trạng thái lỗi | Nhãn bị cắt ở vi |
| **S4** | Lệch nhỏ so với design, chữ chưa dịch | Lệch 2dp |

---

## 9. Báo cáo lượt test (thêm vào cuối file plan, không xoá lượt cũ)

```markdown
## Lượt <n> - <yyyy-mm-dd> - build <versionCode> / <commit hoặc "chưa commit">
- Máy: <model, API, độ phân giải>. Ngôn ngữ: en, vi.
- Unit test: `./gradlew :app:testDebugUnitTest` → <số> test, <pass/fail>.
- Tổng: <x> ca · ✅ <a> · ❌ <b> · ⚠️ <c> · ⏭ <d>. Exit criteria: <đạt / chưa đạt, lý do>.

### Lỗi
| # | Ca | Mức | Mô tả (bước → thấy gì, kỳ vọng gì) | Nguyên nhân (`file:dòng`) | Trạng thái |
|---|---|---|---|---|---|
| 1 | F-27 | S2 | ... | `DrumLessonResultActivity.kt:53` | Đã sửa lượt <n> / Chưa sửa |

### Số liệu
| Ca | Chỉ số | Giá trị |
|---|---|---|
| M-01 | Activities / Views / PSS trước → sau | 1/120/80MB → 1/120/82MB |

### Ghi chú / ⚠️
- ...
```

Ảnh chụp, file dump đặt ở `build/test-evidence/<tính năng>/<lượt>/` (không commit). Trong báo cáo ghi tên file.

---

## 10. Mẫu file plan (copy để bắt đầu)

```markdown
# Test plan: <Tính năng>
Viết theo [TEST_FORM.md](TEST_FORM.md).

## 0. Thông tin
- Phạm vi (đường dẫn code): ...
- Ngoài phạm vi: ...
- Tài liệu đối chiếu: spec / Figma node / PROGRESS mục ...
- Máy / môi trường: (mục 5)
- Dữ liệu chuẩn bị: ...

## 1. Phân tích (mục 2 của form)
### 1.1 Sơ đồ màn
### 1.2 Intent → ca
### 1.3 Effect → ca
### 1.4 State / trạng thái hiển thị → ca
### 1.5 Tài nguyên sống lâu → ca
### 1.6 Dữ liệu lưu → ca
### 1.7 Đầu vào ngoài → ca
### 1.8 Ngưỡng / công thức → ca
### 1.9 Luồng song song → ca
### 1.10 Rủi ro đọc từ code → ca
### 1.11 Khác biệt đã chốt so với spec / app mẫu → ca

## 2. Checklist chung theo màn (ma trận K1–K15)

## 3. Ca test theo nhóm (A, F, S, L, C, M, U, P, D, Q, N, H, X, G, V, T)

## 4. Bộ smoke (danh sách ID P0)

## 5. Báo cáo các lượt
```
