# CLAUDE.md

**HiDoan Base Android**: base app Android (Kotlin, XML + ViewBinding, một module `app`) chạy đủ luồng Splash →
Language → Onboarding → Main → Settings, có sẵn AdMob Next-Gen, Firebase Remote Config, Adjust, Google Play Billing.
App mới = copy base → đổi định danh → áp Figma → thêm màn tính năng. Repo: github.com/chodoanxincode/BaseAppAndroid.

## 1. Đọc gì, theo thứ tự (dừng khi đủ hiểu)

| Tình huống | Đọc |
|---|---|
| Phiên mới của app đang làm | `docs/<app>/PROGRESS.md` **mục 0 "Trạng thái nhanh"** → làm tiếp "Bước tiếp theo" |
| Bắt đầu app mới / phase mới | `docs/APP_PLAYBOOK.md` (quy trình đầy đủ, mục 14 = khi nào hỏi) |
| Trước khi viết code | `docs/CODING_RULES.md` (viết gọn, đúng lớp, cách ghi chú) |
| Đụng quảng cáo / Remote Config / màn base | `docs/BASE_GUIDE.md`, `docs/remote-config-v3/README.md`, `docs/billing.md` |
| Test một tính năng | `docs/testing/TEST_FORM.md` (ví dụ: `docs/testing/examples/drum.md`) |
| Máy mới / lệnh gradle, adb lỗi | `bash tools/dev_env.sh` → `source .dev-env` |

## 2. Làm việc với người dùng

- Trả lời, comment, docs, commit: **tiếng Việt**. Trả lời **ngắn, gạch đầu dòng**: kết quả → chưa làm / rủi ro → cần
  quyết → bước tiếp. Không kể lại từng bước đã làm.
- **Tự làm hết một phase rồi mới báo** (code + unit test + chạy máy thật + smoke P0 + cập nhật PROGRESS). Chỉ dừng giữa
  chừng khi gặp trường hợp phải hỏi (playbook mục 14): spec / Figma / app mẫu mâu thuẫn, thiếu thông tin chỉ người dùng
  có, việc khó hoàn tác, muốn đổi quyết định đã chốt.
- Hỏi bằng AskUserQuestion, **gộp câu**, mỗi câu có phương án đề xuất `(Recommended)`. Câu trả lời ghi ngay vào
  PROGRESS "Quyết định đã chốt", không hỏi lại.
- Không hỏi điều đã có trong PROGRESS / playbook / file này. Không đoán số liệu (màu từ Figma API, hằng số từ code app
  mẫu).

## 3. Mặc định cho app mới (đã chốt, không hỏi lại)

| Chủ đề | Mặc định |
|---|---|
| applicationId | `com.hidoan.<app>` (vd. `com.hidoan.beatmaker`) |
| Ngôn ngữ | **Chỉ en**. Thêm ngôn ngữ khi người dùng yêu cầu (không tạo `values-<x>` mới). Chữ vẫn phải nằm trong `strings.xml` |
| UI | XML + ViewBinding, không Compose |
| Kiến trúc | Luôn Clean Architecture + MVVM/MVI + **Hilt + Room** (playbook mục 10) |
| Quảng cáo | **Tắt ở mọi màn** tới khi người dùng đưa danh sách vị trí. Giữ nguyên hạ tầng `core/ads` |
| Nội dung app mẫu (âm thanh, ảnh, dữ liệu) | Dùng tạm để chạy thử, ghi danh sách vào PROGRESS "Lưu ý", **phải thay trước phát hành** |
| Firebase | `app/google-services.json` là **file mẫu** (giá trị `replace-with-...`) để build được. Đổi `package_name` theo app; người dùng thay file thật sau |
| AdMob / Adjust / Billing ID | Giữ test ID tới khi người dùng đưa ID thật |
| Phát hành (keystore, ký, AAB, Play) | **Người dùng tự làm.** Agent chỉ đảm bảo build release (minify) chạy được; không đụng keystore |
| Test trước khi báo xong phase | Máy thật đang cắm + đạt bộ smoke P0 của test plan + `check_crash.sh` sạch |

## 4. Git

- Nhánh `main`. **Tự commit sau mỗi phần xong** (một việc rõ ràng, build + unit test pass), message tiếng Việt một dòng
  + dòng attribution. **Chỉ push khi người dùng bảo.**
- Không commit: `.idea/`, `build/`, `local.properties`, `.dev-env`, ảnh test (`build/test-evidence/`), APK.
- Đổi schema Room thì commit kèm file trong `app/schemas/`.
- Không `--force`, không sửa lịch sử đã push, không `--no-verify`.

## 5. Quy tắc code (tóm tắt, chi tiết ở `docs/CODING_RULES.md`)

- Làm đúng yêu cầu, cách đơn giản nhất mà đúng; grep cái đã có rồi dùng lại; không thêm abstraction / tham số "phòng khi".
- `domain/` Kotlin thuần (không `android.*`); `data/` cài đặt (Room, file, SDK) + mapper chịu lỗi; `feature/<màn>/`
  Activity + ViewModel (`MviViewModel`) + Contract (State / Intent / Effect). View chỉ gửi Intent và vẽ theo State.
- Player / SoundPool / MediaPlayer: mỗi ViewModel một instance, release ở `onCleared`; `onStop` dừng âm thanh.
- Không IO nặng trên main; dữ liệu ngoài đọc chịu lỗi (bỏ dòng hỏng, không crash); một nguồn sự thật cho mỗi ngưỡng /
  công thức.
- Màu dùng token Figma (`@color/`), chữ dùng `TextAppearance.App.*`, kích thước theo tỉ lệ khi phần tử co theo màn.
- Code chỉ cho phát triển đặt ở `app/src/debug/`.

## 6. Quy tắc của base (bắt buộc)

- Base chỉ chứa màn chung. Không thêm màn / placement theo tính năng riêng của một app vào base.
- Màn mới kế thừa `BaseActivity` / `BaseFragment` / `BaseDialogFragment`; quảng cáo chỉ qua helper `loadNative`,
  `loadBanner`, `showInterstitial`, `showReward`; cấu hình quảng cáo chỉ đọc qua `AdPolicy`. Không gọi SDK AdMob trực
  tiếp trong màn.
- Remote Config: app luôn đọc cùng các key `ad_settings`, `ad_placements`, `ad_ids`, `ad_templates` (+ `ad_config` gộp).
  **Không** tạo key theo version, **không** thêm `app_version` để chọn config. Tách theo bản bằng Firebase Condition
  "App build ≥ N"; `versionCode` tăng mỗi bản.
- Native template: placement chọn mẫu ở trường `template` (không phải `format`). Dòng mẫu có sẵn trong `ad_templates`
  chỉ đè style, `base` bị bỏ qua. Bố cục mới = thêm `ad_tpl_*.xml` + enum `NativeAdTemplate` + danh sách mẫu của sheet /
  script export. Giữ đúng view id chung; không vi phạm chính sách native AdMob (nhãn Ad, ảnh ≥ 120dp, CTA bấm được).
  Không hiện giá ("FREE").
- Thêm placement: `AdKey` + `AdUnit` + `NativePlacement` / `InterstitialPlacement` / `BannerPlacement` (+ `isEnabled`),
  sheet, bảng mặc định trong README theo `docs/BASE_GUIDE.md` mục 4.2.
- Đổi config / placement / template thì cập nhật cùng lúc: code, sheet mẫu + `export_ad_config.gs`, JSON mẫu, docs.
- Quảng cáo lỗi / thiếu config / không fill → app vẫn đi tiếp, không treo, không crash.

## 7. Ghi chú cho phiên sau (ít token)

- Mỗi app có `docs/<app>/PROGRESS.md` theo mẫu playbook 16.1. **Mục 0 "Trạng thái nhanh" ≤ 12 dòng**: phase, đang dở
  (file / class), bước tiếp theo, chờ người dùng, lần test gần nhất, lỗi đang mở.
- Cập nhật sau **mỗi phần** và **trước khi dừng giữa chừng**. Một ý một dòng, ghi đường dẫn file thay vì giải thích code;
  phase xong thì nén ≤ 8 dòng; cả file ≤ 250 dòng. Chi tiết: `docs/CODING_RULES.md` mục 8.
- Lỗi mới đáng nhớ (công cụ, máy, SDK) → thêm vào bảng `docs/APP_PLAYBOOK.md` mục 13.
- Cải tiến dùng chung (docs quy trình, script, helper `core/`) làm ở app con thì **chép ngược về base này**.

## 8. Lệnh

```bash
bash tools/dev_env.sh && source .dev-env    # máy mới / phiên mới: JAVA_HOME, ANDROID_HOME, $ADB, $DEXDUMP, $JADX
./gradlew :app:testDebugUnitTest -q         # unit test
./gradlew :app:assembleDebug -q             # APK debug
./gradlew :app:installDebug                 # cài bản debug
"$ADB" shell am start -n com.hidoan.base/.debug.NativePreviewActivity        # xem các mẫu native
docs/testing/scripts/check_crash.sh         # crash / ANR / exception của app từ lần `logcat -c`
docs/testing/scripts/ui_dump.sh dump.xml    # cây view + báo view tràn
docs/testing/scripts/leak_check.sh <nhãn>   # đếm Activity / View / PSS sau khi ép GC
```

- Máy chặn `adb shell input` (MIUI): dùng broadcast `<pkg>.debug.TAP|TAPS|SWIPE|TYPE|BACK` (`AdbDriver.kt`).
- Override Remote Config trên máy: Settings → Debug config (bản debug).

## 9. Không được làm

- Đổi cài đặt hệ thống / bảo mật của máy test (`wm size`, density, font scale, developer options). Xoá dữ liệu của app
  khác trên máy test khi chưa hỏi.
- Push, force push, tạo / xoá repo, đăng lên dịch vụ ngoài khi chưa được bảo.
- Gắn quảng cáo, thêm ngôn ngữ, thêm thư viện lớn / quyền nhạy cảm khi chưa được yêu cầu.
- Đụng keystore, file ký, thông tin phát hành.
