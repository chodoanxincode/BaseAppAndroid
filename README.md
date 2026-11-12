# HiDoan Base Android

Base app Android dùng để **khởi tạo nhanh các app mới**. Clone về là có sẵn một app chạy đủ luồng
Splash → Language → Onboarding → Home → Settings, đã tích hợp quảng cáo, Remote Config, đo lường và mua Premium.
App mới chỉ cần đổi định danh, áp giao diện theo Figma rồi thêm các màn tính năng.

Repo có kèm **bộ quy trình viết cho AI agent (Claude Code)**: copy base → import design Figma → đọc spec → decompile app
mẫu → dựng kiến trúc → code từng phase → báo cáo → test. Agent đọc các file đó là tự làm được, chỉ hỏi khi gặp mâu
thuẫn.

---

## Có sẵn gì

| Nhóm | Nội dung |
|---|---|
| Luồng màn | Splash, chọn ngôn ngữ, Onboarding, Home, Settings, Premium, Welcome back |
| Quảng cáo | AdMob (Next-Gen SDK): App Open, Interstitial, Native (nhiều template), Banner / Collapsible, Rewarded. Bật / tắt và đổi cấu hình từng vị trí qua Remote Config |
| Cấu hình từ xa | Firebase Remote Config (`ad_settings`, `ad_placements`, `ad_ids`, `ad_templates`), sheet mẫu + script export |
| Đo lường | Adjust (doanh thu quảng cáo, IAP) |
| Mua hàng | Google Play Billing (gói Premium, khôi phục) |
| Kiến trúc | Màn kế thừa `BaseActivity` / `BaseFragment` / `BaseDialogFragment`; `core/mvi` (MVVM + MVI) cho tính năng mới |
| Debug | Màn Debug config (đè Remote Config), xem trước template native, `AdbDriver` điều khiển máy qua adb |
| Quy trình & test | Playbook làm app, quy tắc code, form test + script kiểm crash / leak / UI |

**Kỹ thuật:** Kotlin, ViewBinding, một module `app`, `minSdk 24`, `targetSdk 37`, AGP 9.

---

## Bắt đầu nhanh

### Máy mới (làm một lần)

1. Cài **Android Studio**, **Git**, **Python 3**, **JADX** (bản with-jre). Chi tiết ở `docs/APP_PLAYBOOK.md` mục 2.1.
2. Clone repo rồi dò môi trường:
   ```bash
   bash tools/dev_env.sh      # tìm JDK, Android SDK, adb, dexdump, JADX, Python → ghi .dev-env
   source .dev-env
   ```
3. Build và chạy thử:
   ```bash
   ./gradlew :app:testDebugUnitTest      # unit test
   ./gradlew :app:installDebug           # cài bản debug lên máy đang cắm
   ```

### Làm một app mới từ base

Mở Claude Code trong repo app mới (đã copy từ base) và nhắn:

> Làm app mới theo `docs/APP_PLAYBOOK.md`. Figma: `<link>`, app mẫu: `<package / APK>`, package: `<applicationId>`.

Hoặc làm tay theo các bước trong `docs/APP_PLAYBOOK.md`: đổi package → import token Figma → dựng flow chung → đọc spec →
decompile app mẫu → dựng Hilt / Room → làm từng phase → test.

---

## Tài liệu

| File | Đọc khi |
|---|---|
| `CLAUDE.md` | Quy tắc bắt buộc (agent tự nạp mỗi phiên) |
| `docs/APP_PLAYBOOK.md` | Làm app mới / làm một phase: quy trình đầy đủ, mẫu PROGRESS, lỗi hay gặp |
| `docs/CODING_RULES.md` | Trước khi viết code: viết gọn, đúng lớp, cách ghi chú cho phiên sau |
| `docs/BASE_GUIDE.md` | Base có gì, viết màn mới, quảng cáo, Remote Config, template native |
| `docs/remote-config-v3/` | Từng parameter Remote Config, sheet mẫu, JSON mẫu, script export |
| `docs/billing.md` | Premium / Google Play Billing |
| `docs/testing/TEST_FORM.md` | Cách test một tính năng; ví dụ đầy đủ ở `docs/testing/examples/drum.md` |

## Cấu trúc thư mục

```
app/src/main/java/com/hidoan/base/
├── HiDoanBaseApp.kt     khởi tạo Adjust, quảng cáo, Premium, Remote Config
├── core/
│   ├── ads/             quảng cáo (chỉ dùng qua helper của BaseActivity / BaseFragment)
│   ├── billing/         Premium, Google Play Billing
│   ├── config/          Remote Config, AdKey, PlacementConfig
│   ├── language/        ngôn ngữ app
│   ├── mvi/             MviViewModel, collectWhenStarted
│   ├── navigation/ preferences/
│   └── ui/base/         BaseActivity, BaseFragment, BaseDialogFragment, adapter
└── feature/             splash, language, onboarding, main, settings, premium, welcome, ads, debug
app/src/debug/           NativePreviewActivity, AdbDriver (chỉ có ở bản debug)
docs/                    tài liệu (bảng trên)
tools/dev_env.sh         dò môi trường máy
skills/                  skill cho agent: decompile APK đã cài, thiết kế UI mobile
```

## Quy tắc chính

- Base **chỉ chứa màn chung**. Màn và vị trí quảng cáo riêng của từng app nằm ở app con.
- Quảng cáo chỉ gọi qua helper (`loadNative`, `loadBanner`, `showInterstitial`, `showReward`); cấu hình chỉ đọc qua
  `AdPolicy`. Quảng cáo lỗi hoặc không có thì app vẫn chạy tiếp, không treo, không crash.
- Remote Config không tạo key theo version. Tách cấu hình theo bản bằng Firebase Condition "App build ≥ N".
- Code chỉ dùng khi phát triển đặt trong `app/src/debug/`.
- Comment, tài liệu và commit viết bằng tiếng Việt.

## Trước khi dùng cho app thật

- [ ] Thay `app/google-services.json` bằng file từ **Firebase project của bạn** (file hiện tại chỉ để build được).
- [ ] Điền AdMob app ID, `ad_ids`, Adjust token, Billing product ID thật (`docs/BASE_GUIDE.md` mục 9).
- [ ] Đổi `applicationId`, tên app, icon theo `docs/APP_PLAYBOOK.md` mục 5.
