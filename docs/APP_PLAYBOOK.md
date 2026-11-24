# Playbook: làm một app mới từ base

File này mô tả **toàn bộ quy trình** làm một app, từ lúc copy base tới lúc test xong từng tính năng, rút ra từ cách đã
làm Beat Maker (T04). Agent nhận một dự án mới (hoặc một phiên mới của dự án đang làm) đọc file này là đủ để tự làm.
Chỉ hỏi lại người dùng trong các trường hợp ở **mục 14**.

## 0. Cách dùng file này

**Dự án mới:** đọc hết file này một lượt → làm từ mục 4 (Giai đoạn 0) trở đi, theo đúng thứ tự.

**Máy mới:** làm mục 2 trước (cài công cụ + `bash tools/dev_env.sh`).

**Phiên mới của dự án đang làm:** đọc theo thứ tự, dừng khi đã đủ hiểu:
1. `CLAUDE.md` (tự nạp);
2. `docs/<app>/PROGRESS.md` **mục 0 "Trạng thái nhanh"**. Chỉ đọc tiếp các mục dưới khi cần;
3. `docs/CODING_RULES.md` trước khi viết code;
4. các mục của file này ứng với việc sắp làm;
5. `docs/BASE_GUIDE.md` nếu đụng tới quảng cáo / Remote Config / màn base;
6. `docs/testing/TEST_FORM.md` khi test.

**Quy ước trong file:** `<App>` là tên app (vd. Beat Maker), `<app>` là slug thư mục (vd. `beat-maker`), `<MÃ>` là mã
dự án (vd. T04), `<pkg>` là applicationId (vd. `com.wavez.music.beatmaker`), `<pkgdir>` là đường dẫn package
(vd. `com/wavez/music/beatmaker`).

**Nguyên tắc chung:**
- Trả lời, comment, docs, commit đều viết **tiếng Việt**, ngắn, nói "vì sao" chứ không lặp lại code.
- Làm theo bảng "Quyết định đã chốt" trong PROGRESS. Chưa có quyết định thì: **luồng / chức năng / core theo app mẫu,
  hình ảnh / màu / font / icon theo Figma**.
- Mỗi lần xong một phần: cập nhật PROGRESS rồi **tự commit** (push chỉ khi người dùng bảo). Xong cả phase mới báo người
  dùng (mục 15). Các mặc định đã chốt (package, ngôn ngữ, quảng cáo, Firebase, phát hành): `CLAUDE.md` mục 3.
- Không đoán số liệu: màu lấy từ Figma API, hằng số lấy từ code app mẫu, kích thước lấy từ node Figma.

---

## 1. Tổng quan các giai đoạn

| # | Giai đoạn | Người dùng đưa | Đầu ra | Xong khi | Mục |
|---|---|---|---|---|---|
| 0 | Nhận dự án | Tên app, package, mã dự án, link Figma, app mẫu, phạm vi | Phần "Thông tin dự án" của PROGRESS | Đủ thông tin ở bảng 4.1 | 4 |
| 1 | Copy base, đổi định danh | Đường dẫn base | Repo mới build được | Build + unit test pass, mở được app trên máy | 5 |
| 2 | Import design system | Link Figma (trang Styles & Foundations) | Màu, font, kiểu chữ, `docs/design-system.md`, `tools/gen_figma_tokens.py` | Resource sinh tự động, build pass | 6 |
| 3 | Dựng flow chung theo Figma | Node Splash / Language / Onboarding / Home | Màn chung đúng design | Chạy máy thật, chụp so với Figma | 7 |
| 4 | Đọc spec | Section spec trên Figma | Ghi chú spec + câu hỏi mở | Đã hỏi và chốt mọi điểm mâu thuẫn | 8 |
| 5 | Decompile app mẫu | APK (hoặc app đã cài trên máy test) | `<APP>_ANALYSIS.md` | Hiểu core mọi tính năng trong phạm vi | 9 |
| 6 | Kiến trúc + kế hoạch | — | Hilt / Room / MVI dựng sẵn, PROGRESS có danh sách phase | Build pass | 10 |
| 7 | Làm từng phase | Chốt quyết định khi được hỏi | Code + unit test + PROGRESS cập nhật | Định nghĩa "xong" ở mục 11.3 | 11 |
| 8 | Test từng tính năng | — | `docs/testing/<tính-năng>.md` + báo cáo lượt test | Exit criteria của TEST_FORM | 12 |

Giai đoạn 4 và 5 làm song song được (đọc spec để biết cần soi gì trong app mẫu). Giai đoạn 7 và 8 lặp lại cho từng phase.

---

## 2. Môi trường máy (máy mới làm một lần)

### 2.1 Cài công cụ

| Công cụ | Bản đã dùng | Lấy ở đâu | Ghi chú |
|---|---|---|---|
| Android Studio | bản ổn định mới nhất | developer.android.com/studio | Có sẵn JDK (`jbr`). Mở SDK Manager cài: SDK Platform theo `compileSdk`, Platform-Tools, Build-Tools mới nhất |
| Git | bất kỳ | git-scm.com | Windows: dùng Git Bash cho các script `.sh` |
| JADX | 1.4.7 (with-jre) | github.com/skylot/jadx/releases | Giải nén vào `~/Downloads/jadx*/` hoặc thêm vào PATH |
| Python | 3.x | python.org | Không dùng bản "giả" của Microsoft Store |
| Claude Code + plugin Figma | — | `/plugin` trong Claude Code | Đăng nhập Figma (`whoami` của MCP phải ra tài khoản) |
| Máy Android thật | API ≥ `minSdk` | — | Bật USB debugging; máy MIUI bật thêm "USB debugging (Security settings)" nếu được, không thì dùng AdbDriver |

### 2.2 Dò môi trường (tự động)

```bash
bash tools/dev_env.sh      # dò JDK, Android SDK, adb, dexdump, JADX, Python → ghi .dev-env (không commit)
source .dev-env            # trước mọi lệnh gradle / adb trong phiên
```

Script in ra đường dẫn đã tìm thấy, thiết bị đang cắm, và mục **THIẾU** (nếu có). Thiếu thì cài theo bảng 2.1 rồi chạy
lại. Không tìm được mà công cụ đã cài ở chỗ lạ thì hỏi người dùng đường dẫn rồi thêm vào danh sách dò trong script.
`docs/testing/scripts/env.sh` tự đọc `.dev-env` (chưa có thì tự chạy `dev_env.sh`).

### 2.3 Lệnh hay dùng

```bash
source .dev-env
./gradlew :app:testDebugUnitTest -q                   # unit test
./gradlew :app:assembleDebug -q                       # APK debug → app/build/outputs/apk/debug/app-debug.apk
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" shell am start -n <pkg>/.feature.splash.SplashActivity
"$ADB" exec-out screencap -p > shot.png               # chụp màn rồi mở bằng Read để xem
docs/testing/scripts/ui_dump.sh dump.xml              # cây view + bounds + báo tràn
"$ADB" logcat -c; ...; docs/testing/scripts/check_crash.sh
"$ADB" shell run-as <pkg> ls -la files shared_prefs databases   # dữ liệu app (bản debug)
"$DEXDUMP" -d classes.dex > dump.txt                  # bytecode app mẫu
```

Lưu ý Windows: Git Bash tự đổi `/data/...` thành `C:/Program Files/Git/data/...`. `.dev-env` đã đặt
`MSYS_NO_PATHCONV=1`, nên luôn `source .dev-env` trước khi chạy lệnh adb có đường dẫn.

---

## 3. Bản đồ tài liệu của một app

| File | Ai viết | Nội dung |
|---|---|---|
| `CLAUDE.md` | GĐ 1, cập nhật mỗi khi thêm quy tắc | Quy tắc bắt buộc + phần riêng của app (mẫu 16.4) |
| `docs/BASE_GUIDE.md` | có sẵn từ base, sửa khi đổi màn base | Base có gì, quảng cáo, Remote Config |
| `docs/APP_PLAYBOOK.md` | file này, giữ nguyên giữa các app | Quy trình |
| `docs/CODING_RULES.md` | giữ nguyên giữa các app | Cách viết code, cách ghi chú cho phiên sau (ít token) |
| `tools/dev_env.sh` → `.dev-env` | chạy trên mỗi máy, `.dev-env` không commit | Đường dẫn JDK, adb, dexdump, JADX, Python |
| `docs/design-system.md` | GĐ 2 | Token Figma → resource |
| `docs/<app>/PROGRESS.md` | GĐ 0, cập nhật liên tục | Tiến độ, quyết định, việc tiếp theo (mẫu 16.1) |
| `docs/<app>/SPEC_NOTES.md` | GĐ 4 | Ghi chú spec theo màn (mẫu 16.2) |
| `~/Downloads/Decompile-<App>/<APP>_ANALYSIS.md` | GĐ 5 | Phân tích app mẫu (mẫu 16.3). Đặt ngoài repo vì là code của bên khác. |
| `docs/testing/TEST_FORM.md` + `scripts/` | giữ nguyên giữa các app | Form test |
| `docs/testing/<tính-năng>.md` | GĐ 8 | Test plan + báo cáo từng tính năng |

---

## 4. Giai đoạn 0: Nhận dự án

### 4.1 Thông tin cần có

Thiếu thông tin thì hỏi **gộp một lần** (một AskUserQuestion, tối đa 4 câu, mỗi câu có phương án đề xuất). Thông tin
nào có giá trị mặc định hợp lý thì tự dùng mặc định và ghi lại, không hỏi.

| Thông tin | Ví dụ (T04) | Mặc định nếu không có | Dùng ở |
|---|---|---|---|
| Tên app hiển thị | Beat Maker | — (phải hỏi) | `app_name` |
| applicationId | `com.wavez.music.beatmaker` | `com.hidoan.<app>` | GĐ 1 |
| Mã dự án | T04 | lấy từ tên file Figma | tên thư mục, commit |
| Base copy từ đâu | repo T03 Shimeji Pets | repo app gần nhất mà người dùng chỉ | GĐ 1 |
| Figma: fileKey, trang Styles, node từng màn, section spec | `Mfzn6xxgWzcLXj6udomLZd`, spec `10334:15927`, Home `10009:11950` | tự tìm bằng `get_metadata` nếu có fileKey | GĐ 2–4 |
| App mẫu: package, version, nơi có APK | `com.beatmaker.drumpad.playguitar.playpiano` v1.1.3 | kéo từ máy test nếu đã cài | GĐ 5 |
| Phạm vi tính năng lần này | Pad, Customize, Lesson, REC, Drum; không Piano / Guitar | — (phải hỏi) | GĐ 6 |
| Quảng cáo | tạm tắt ở mọi màn mới tới khi có yêu cầu | tạm tắt, giữ hạ tầng | GĐ 3, 7 |
| Ngôn ngữ | en + vi | **chỉ en** (thêm khi được yêu cầu) | strings |
| Hướng màn hình | dọc; Drum ngang | theo Figma | manifest |

### 4.2 Việc làm ngay

1. Tạo `docs/<app>/PROGRESS.md` theo mẫu 16.1, điền phần đầu (Figma, app mẫu, base) và mục "Quyết định đã chốt" với các
   mặc định đã dùng.
2. Thêm phần app vào `CLAUDE.md` (mẫu 16.4).

---

## 5. Giai đoạn 1: Copy base, đổi định danh

### 5.1 Copy

```powershell
# PowerShell: copy base sang thư mục mới, bỏ các thư mục build / IDE / git
robocopy <thư mục base> <thư mục cha>\<mã>-<app> /E /XD .git .idea build .gradle .kotlin /XF local.properties
```

Rồi trong thư mục mới: `git init`, `git add -A`, commit đầu, message
`Khởi tạo <App> từ base của <app base>`.

### 5.2 Đổi định danh (một commit riêng: `Đổi package name thành <pkg>`)

Base HiDoan dùng package `com.hidoan.base`, tên riêng `HiDoanBase` (class `HiDoanBaseApp`, theme `Theme.HiDoanBase`, `rootProject.name`). App mới đổi cả hai. Danh sách đầy đủ phải đổi:

| Nhóm | Chỗ đổi |
|---|---|
| Gradle | `app/build.gradle.kts`: `namespace`, `applicationId` |
| Firebase | `app/google-services.json`: `package_name` phải khớp applicationId, nếu không build lỗi "No matching client found". Base có sẵn **file mẫu** (giá trị `replace-with-...`): chỉ đổi `package_name`, người dùng thay file thật sau. Ghi vào PROGRESS "Lưu ý". |
| Thư mục code | Chuyển `app/src/{main,debug,test,androidTest}/java/<pkgdir cũ>` sang `<pkgdir mới>` (dùng `git mv` để giữ lịch sử) |
| Khai báo package | Mọi dòng `package ...` và `import <pkg cũ>...` trong `.kt` (main, debug, test, androidTest) |
| Layout dùng tên class đầy đủ | `<pkg cũ>.xxx.View` trong `res/layout/*.xml` (custom view, template ad `ad_tpl_*.xml`, `ad_native.xml`) |
| Manifest | `android:name` dạng đầy đủ (nếu có), `authorities="${applicationId}..."` (tự đổi), Application class |
| Debug | Tiền tố action trong `app/src/debug/.../AdbDriver.kt` (`<pkg>.debug.`) |
| Tên Application | `XxxApp.kt` → `<App>App.kt` + manifest |
| Docs | `CLAUDE.md` (lệnh `adb shell am start -n <pkg>/...`, action AdbDriver), `docs/BASE_GUIDE.md` (dòng "Đường dẫn code tính từ...") |
| Hiển thị | `app_name` (`values/` và mọi `values-<x>/` đang có), icon launcher (`mipmap-*`), theme name nếu muốn (`Theme.<App>`) |

Lệnh gợi ý (Git Bash, chạy từ gốc repo):

```bash
OLD=com.hidoan.base; NEW=<pkg>          # vd. com.wavez.music.beatmaker
OLD_DIR=${OLD//.//}; NEW_DIR=${NEW//.//}
for s in main debug test androidTest; do
  d=app/src/$s/java; [ -d "$d/$OLD_DIR" ] || continue
  mkdir -p "$(dirname "$d/$NEW_DIR")"; git mv "$d/$OLD_DIR" "$d/$NEW_DIR"
done
grep -rl "$OLD" app CLAUDE.md docs --include=*.kt --include=*.kts --include=*.xml --include=*.json --include=*.md \
  | xargs sed -i "s/${OLD//./\\.}/$NEW/g"
grep -rn "$OLD" app CLAUDE.md docs | head      # phải rỗng
```

### 5.3 Kiểm

- [ ] `grep -rn "<pkg cũ>"` không còn kết quả (trừ ghi chú lịch sử).
- [ ] `./gradlew :app:testDebugUnitTest :app:assembleDebug` pass.
- [ ] Cài lên máy: Splash → Language → Onboarding → Home → Settings chạy được.
- [ ] Broadcast AdbDriver với tiền tố mới chạy được (`am broadcast -a <pkg>.debug.BACK`).

Các mục còn lại của `BASE_GUIDE.md` mục 9 (AdMob ID, Adjust token, Billing product) **chỉ làm khi người dùng đưa ID**.
Chưa có thì giữ test ID và ghi vào PROGRESS "Lưu ý / trước phát hành".

---

## 6. Giai đoạn 2: Import design system từ Figma

### 6.1 Đọc token nguyên văn

1. `create_session(fileKey, "Import design tokens")`.
2. Nạp skill `figma:figma-use`, rồi gọi `use_figma` với script đọc biến / style (Plugin API):

```js
// Màu: mọi collection, mọi mode, giữ alias
const cols = await figma.variables.getLocalVariableCollectionsAsync();
const out = [];
for (const c of cols) {
  for (const id of c.variableIds) {
    const v = await figma.variables.getVariableByIdAsync(id);
    for (const m of c.modes) {
      const val = v.valuesByMode[m.modeId];
      out.push({ collection: c.name, mode: m.name, name: v.name, type: v.resolvedType,
        value: val?.type === 'VARIABLE_ALIAS' ? { alias: (await figma.variables.getVariableByIdAsync(val.id)).name } : val });
    }
  }
}
// Kiểu chữ
const texts = (await figma.getLocalTextStylesAsync()).map(s => ({ name: s.name, font: s.fontName, size: s.fontSize,
  lineHeight: s.lineHeight, letterSpacing: s.letterSpacing, boundVariables: s.boundVariables }));
// Gradient / paint style
const paints = (await figma.getLocalPaintStylesAsync()).map(s => ({ name: s.name, paints: s.paints }));
return { out, texts, paints };
```

   Kết quả lớn thì đọc theo từng collection. Màu trong API là `{r,g,b,a}` 0..1 → đổi sang `#RRGGBB` / `#RRGGBBAA` (Android
   dùng `#AARRGGBB`, script sinh phải đổi thứ tự alpha).
3. **Không** lấy màu bằng cách nhìn ảnh chụp, **không** dùng `get_variable_defs` của một node thay cho toàn bộ
   collection (sẽ thiếu màu).

### 6.2 Sinh resource

`tools/gen_figma_tokens.py` (xem bản T04 làm mẫu): dữ liệu dán thẳng trong script dưới dạng list
`PRIMITIVES = [("color/primary/500", "#7b5cfa"), ...]`, `SEMANTIC = [("surface/surface_dark_1", "color/dark/800"), ...]`,
`TEXT_STYLES = [...]`. Chạy: `python tools/gen_figma_tokens.py app/src/main/res`. Sinh ra:

| File | Nội dung |
|---|---|
| `res/values/colors_figma_primitives.xml` | màu gốc |
| `res/values/colors_figma_semantic.xml` | màu theo vai trò, `@color/<gốc>` (alias); thêm `gradient_<tên>_start/center/end` |
| `res/values/text_appearance_figma.xml` | `TextAppearance.App.<Cỡ>.<Weight>`: `fontFamily`, `textSize` (sp), `lineHeight`, `letterSpacing` (em = px / size) |

Quy tắc tên: bỏ tiền tố `color/`, đổi `/` thành `_`; tên con đã bắt đầu bằng tên nhóm thì bỏ tên nhóm
(`surface/surface_dark_1` → `surface_dark_1`). Không sửa tay file sinh ra. Figma chỉ có một mode thì không tạo
`values-night`.

### 6.3 Font

1. Tải font từ Google Fonts. Variable font thì tách file tĩnh cho **đúng các weight design dùng** (vd. Nunito 400 / 500
   / 600 / 700 / 800), đặt `res/font/<font>_<weight>.ttf`, tên chữ thường, gạch dưới.
2. `res/font/<font>.xml` (font-family gom các weight) để style dùng `@font/<font>` + `android:textFontWeight`.
3. Chép giấy phép (OFL) vào `tools/<Font>-OFL.txt`.
4. Font số / tiêu đề đặc biệt (vd. 42dot Sans ExtraBold ở T04) thì cắt glyph cho nhẹ. **Kiểm glyph tiếng Việt**: thiếu
   thì tạo style cùng tên trong `values-vi/` dùng font khác (T04: `values-vi/text_appearance_display.xml` dùng Nunito
   ExtraBold).

### 6.4 Docs + commit

Viết `docs/design-system.md`: nguồn (fileKey, trang), bảng file sinh ra, quy tắc tên + ví dụ, màu chính, bảng cỡ chữ
(Figma → Android, sp / line height), gradient + cách dùng (`core/ui/TextGradient.kt` cho chữ gradient), lệnh chạy lại
script. Commit (khi được yêu cầu): `Import màu, font và kiểu chữ từ Figma <MÃ> - Design`.

---

## 7. Giai đoạn 3: Dựng màn theo Figma (áp dụng cho mọi màn, cả GĐ 7)

### 7.1 Đọc design

1. `get_metadata(node)` để thấy cây layer, kích thước frame (thường 360 hoặc 375 × 800).
2. Nạp skill `figma:figma-design-to-code`, gọi `get_design_context(node)` lấy thông số; `get_screenshot(node)` lấy ảnh
   đối chiếu.
3. Output MCP (React / Tailwind) chỉ là **tham khảo số liệu**. Dựng lại bằng XML + ViewBinding.

### 7.2 Figma → Android

| Figma | Android |
|---|---|
| Frame có auto layout dọc / ngang | `LinearLayout` (đơn giản) hoặc `ConstraintLayout` + chain |
| Khoảng cách cố định giữa phần tử | margin theo dp, **hoặc** theo tỉ lệ nếu phần tử co theo màn |
| Thẻ / nút có tỉ lệ cố định | `app:layout_constraintDimensionRatio` |
| Vị trí theo % màn hình | `Guideline` (`layout_constraintGuide_percent`), `layout_constraintWidth_percent` |
| Lưới ô vuông co theo màn | view tự tính (T04: `SquareGridLayout`, `PadGridLayout`) |
| Bo góc, viền, nền màu | `shape` drawable (`bg_<tên>.xml`), màu semantic |
| Viền gradient | `layer-list` hoặc shape có gradient (màu `gradient_*`) |
| Chữ gradient | `applyHorizontalTextGradient(start, end)` trong `core/ui/TextGradient.kt` |
| Text style | `android:textAppearance="@style/TextAppearance.App.<Cỡ>.<Weight>"` |
| Ảnh bitmap, ảnh có mask, nền nhiều lớp | `download_assets` → cắt đúng khung → `.webp` trong `drawable-nodpi/`; nhiều lớp cố định thì **ghép sẵn thành một ảnh** |
| Icon, hình phẳng | SVG → VectorDrawable `ic_<tên>.xml` |
| Blur / glow ngoài khung | ảnh blur vẽ sẵn theo khung thẻ (`home_glow_*.webp`); không dùng RenderEffect (API 31+) |
| Inner shadow | layer-list nhiều lớp, hoặc ảnh |
| Ảnh vuông có bo góc mà lộ góc màu | xuất ảnh **tràn màu ra góc**, bo bằng outline / clip của view (lỗi đã gặp ở pad T04) |

### 7.3 Kích thước, thanh hệ thống, hướng màn

- Ưu tiên kích thước **theo tỉ lệ**, hạn chế dp cố định. Kiểm trên màn hẹp 360dp và màn hiện có.
- Chữ có thể dài (vi) thì dùng `autoSizeTextType="uniform"` hoặc `maxLines` + `ellipsize`. Không để chữ bị cắt.
- Thanh hệ thống: T04 đã thêm vào `BaseActivity` (base HiDoan **chưa có**, cần thì chép từ T04 `core/ui/base/` hoặc viết lại theo đúng API này):
  - `darkSystemBars` (override `true` khi nền tối để icon status bar trắng);
  - `systemBarPaddingTarget()` (mặc định `binding.root` được đệm inset; trả `null` khi màn tự xử lý);
  - `View.padForSystemBars()` cho view con khi nền phủ cả status bar.
- Màn ngang dạng chơi (T04 Drum): `android:screenOrientation="sensorLandscape"`, gọi
  `DrumScreens.enterImmersive(activity, root) { left, right -> ... }` để ẩn thanh hệ thống và chừa tai thỏ. **Dialog**
  trên màn immersive phải tự ẩn thanh hệ thống theo màn gọi (`RecordDialog.matchHostSystemBars`), không thì thanh hiện
  lại.

### 7.4 Quy ước tên resource

| Loại | Mẫu tên | Ví dụ |
|---|---|---|
| Layout | `activity_<màn>`, `fragment_<màn>`, `dialog_<tên>`, `item_<tên>`, `popup_<tên>`, `view_<tên>` | `activity_drum`, `item_drum_lesson` |
| Ảnh nền / shape | `bg_<chỗ dùng>` | `bg_home.webp`, `bg_button_primary.xml` |
| Icon | `ic_<tên>` | `ic_drum_nav_play` |
| Ảnh theo tính năng | `<tính-năng>_<tên>` | `drum_classic_kick.webp`, `pad_color_red.webp` |
| Selector màu | `res/color/<chỗ dùng>.xml` | `text_drum_nav.xml` |
| Strings | `<tính-năng>_<tên>` (prefix theo tính năng) | `drum_nav_style`, `record_saved_title` |
| Style chữ riêng | `res/values/text_appearance_<tính-năng>.xml` | `text_appearance_record.xml` |
| Dimens riêng | `res/values/dimens_<tính-năng>.xml` | `dimens_lesson.xml` |

### 7.5 Chữ và ngôn ngữ

- Mọi chữ trong `values/strings.xml` (en). Ngôn ngữ khác chỉ thêm khi người dùng yêu cầu; khi đã có thì thêm chuỗi vào
  mọi `values-<x>/strings.xml`. Không hard-code chữ trong layout / code.
- Đổi locale ở Language: đổi **sau** khi đã mở màn kế tiếp (đổi trước thì màn bị tạo lại giữa chừng và kẹt, lỗi đã gặp).
- Ngôn ngữ app hỗ trợ khai báo trong `core/language/AppLanguage.kt`.

### 7.6 Quảng cáo ở màn

Theo GĐ 0. Tạm bỏ thì: bỏ lời gọi `loadNative` / `showInterstitial` ở màn và view container, cập nhật `BASE_GUIDE.md`
(bảng màn, mục "Còn thiếu") + `docs/remote-config-v3/README.md`, **giữ nguyên** `core/ads`, `AdKey`, `AdUnit`. Gắn thì
theo `BASE_GUIDE.md` mục 3–4.

### 7.7 Kiểm một màn

- [ ] Chụp màn trên máy, đặt cạnh `get_screenshot` của Figma: bố cục, màu, chữ, icon khớp.
- [ ] `ui_dump.sh` không báo tràn; xem bằng mắt không cắt chữ (mọi ngôn ngữ app có).
- [ ] Nút có `contentDescription` (icon không chữ), vùng chạm ≥ 48dp.
- [ ] Không hard-code màu hex / chữ.

---

## 8. Giai đoạn 4: Đọc spec

1. `get_metadata(<section spec>)` để lấy danh sách frame. Đọc **mọi** frame (`get_screenshot`, `get_design_context` khi
   cần chữ). Spec thường gồm: luồng màn, trạng thái từng màn, quy tắc, ghi chú của designer / BA.
2. Viết `docs/<app>/SPEC_NOTES.md` theo mẫu 16.2, mỗi màn một mục:
   - đường vào / ra;
   - các trạng thái (mặc định, loading, rỗng, lỗi, đang chạy…);
   - các thao tác và kết quả;
   - quy tắc / số liệu (giới hạn, điểm, thời gian);
   - **câu hỏi mở**.
3. So spec với app mẫu (GĐ 5) và design. Ba loại câu hỏi phải hỏi:
   - spec **không nói** nhưng ảnh hưởng trải nghiệm / dữ liệu;
   - spec **mâu thuẫn** với design;
   - spec **khác** app mẫu (vd. công thức chấm điểm).
4. Hỏi gộp (AskUserQuestion), mỗi câu 2–4 phương án, phương án đề xuất đặt đầu và ghi `(Recommended)`, mô tả rõ hệ quả.
   Ví dụ các câu đã hỏi ở T04: chấm điểm theo app mẫu hay theo spec; REC thu micro hay mix sample; gián đoạn thì
   tiếp tục hay chơi lại; Drum làm giao diện theo Figma hay theo app mẫu.
5. Ghi câu trả lời vào "Quyết định đã chốt" của PROGRESS **kèm ngày**. Từ đó không hỏi lại.
6. Spec có bản mới: ghi node mới, đánh dấu node cũ đã thay, đọc phần khác biệt, hỏi nếu đụng việc đã làm.

---

## 9. Giai đoạn 5: Decompile app mẫu

Mục tiêu: hiểu **core** (âm thanh, dữ liệu, công thức, hằng số, luồng, xử lý lỗi). **Không** chép giao diện hay chép
nguyên code.

### 9.1 Lấy APK

```bash
source docs/testing/scripts/env.sh
"$ADB" shell pm path <pkg app mẫu>                      # có thể ra nhiều dòng (split APK)
mkdir -p ~/Downloads/Decompile-<App>/apk
"$ADB" pull /data/app/.../base.apk ~/Downloads/Decompile-<App>/apk/   # pull từng dòng
```

Người dùng đưa file `.apk` / `.xapk` thì dùng file đó (xapk là zip, giải nén lấy các apk).

### 9.2 Decompile

1. Mở JADX (`$JADX` trong `.dev-env`) → mở `base.apk` → **File → Save as gradle project** vào
   `~/Downloads/Decompile-<App>/`. Kết quả có `app/src/main/java`, `res`, `assets`, `AndroidManifest.xml`,
   `classes*.dex`.
2. Method JADX báo không dịch được thì đọc bytecode:
   ```bash
   "$DEXDUMP" -d classes.dex > dump.txt
   grep -n "Lpackage/Class;.methodName" dump.txt
   ```
   T04 đã đọc công thức chấm Lesson từ bytecode (`a8/f`, `e7/b0`…) theo cách này.

### 9.3 Tìm core khi code bị R8 làm rối

| Cần tìm | Cách lần |
|---|---|
| Màn của tính năng | `AndroidManifest.xml` (tên Activity không bị đổi), layout trong `res/layout` → `setContentView` / binding |
| Phát âm thanh | grep `SoundPool`, `MediaPlayer`, `ExoPlayer`, `AudioTrack`, `.load(` |
| Ghi âm | `MediaRecorder`, `AudioRecord` |
| Room | `@Entity`, `@Dao`, chuỗi `CREATE TABLE`; tên bảng / cột là chuỗi nên không bị đổi |
| SharedPreferences | `getSharedPreferences(`, chuỗi key (`getInt("...")`) |
| Remote Config / mạng | `FirebaseRemoteConfig`, `getString("key")`, `http`, `.zip`, `OkHttp`, `Retrofit` |
| Dữ liệu sẵn | `assets/`, `res/raw/`; dữ liệu bài học có thể nằm trong class Kotlin (mảng hằng) |
| Công thức, ngưỡng | trong hàm xử lý chạm / kết quả: so sánh với hằng số (`300`, `500`, `0.05f`…) |
| Theme / skin | resource `drawable` theo tiền tố, pref lưu theme |

### 9.4 Viết `<APP>_ANALYSIS.md`

Đặt ở gốc thư mục decompile, theo mẫu 16.3. Bắt buộc:
- mọi khẳng định đều có **đường dẫn class / method** đi kèm;
- hằng số ghi **số thật** (vd. "lệch ≤ 300 ms Perfect");
- method không đọc được thì ghi ⚠️ và cách đã suy ra;
- mỗi tính năng có mục "nên giữ / nên làm khác".

### 9.5 Dùng nội dung của app mẫu

- Âm thanh, bài học, MIDI, ảnh của app mẫu chỉ dùng **tạm** để chạy thử. Chuyển sang định dạng của mình (vd. dữ liệu
  bài trong class → `assets/lessons/lessons.json`) để sau này thay dễ.
- Ghi vào PROGRESS mục "Lưu ý": **phải thay trước phát hành** (bản quyền), kèm danh sách file.
- Link tải của app mẫu (vd. R2) chỉ dùng tạm; đặt trong `buildConfigField`, ghi chỗ đổi.

---

## 10. Giai đoạn 6: Kiến trúc và kế hoạch

### 10.1 Thêm thư viện (base không có DI)

1. `gradle/libs.versions.toml`: thêm version + library + plugin cho **Hilt**, **KSP**, **Room** (T04: Hilt 2.59.2, KSP
   2.3.6, Room 2.8.4; dùng bản mới ổn định nhất lúc làm, ghi vào "Quyết định đã chốt").
2. `build.gradle.kts` gốc: `alias(libs.plugins.hilt.android) apply false`, `alias(libs.plugins.ksp) apply false`.
3. `app/build.gradle.kts`:
   ```kotlin
   plugins { alias(libs.plugins.ksp); alias(libs.plugins.hilt.android) }
   android { ksp { arg("room.schemaLocation", "$projectDir/schemas") }; buildFeatures { buildConfig = true } }
   dependencies {
       implementation(libs.hilt.android); ksp(libs.hilt.compiler)
       implementation(libs.androidx.room.runtime); implementation(libs.androidx.room.ktx); ksp(libs.androidx.room.compiler)
   }
   ```
4. Application: `@HiltAndroidApp`. `BaseActivity.onCreate` / `onDestroy` **bỏ `final`** (Hilt sinh lớp cha cần
   override). Màn con vẫn chỉ dùng hook `initConfig` → `initViews` → `initListeners` → `observeData` →
   `releaseResources`.
5. `core/mvi/MviViewModel.kt` + `core/mvi/FlowCollect.kt` **đã có sẵn trong base**. Thêm `di/AppModule.kt`, `di/BindingModule.kt`.
6. Build + chạy unit test, cài máy kiểm luồng base vẫn chạy.

### 10.2 Cấu trúc thư mục

```
<pkg>
├── <App>App.kt          @HiltAndroidApp
├── di/
│   ├── AppModule.kt     @IoDispatcher, @ApplicationScope (SupervisorJob + handler log lỗi), Room DB + Dao, config từ BuildConfig
│   └── BindingModule.kt @Binds interface domain → Impl ở data
├── core/                dùng chung, không theo tính năng
│   ├── mvi/             MviViewModel, collectWhenStarted
│   ├── ads/ billing/ config/ language/ navigation/ preferences/   (từ base)
│   ├── network/         NetworkChecker
│   ├── permission/      StorageAccess (quyền theo API)
│   ├── time/            MonotonicClock (inject, test giả được)
│   └── ui/              base/ (BaseActivity…), AppToast, TextGradient
├── domain/              Kotlin thuần: KHÔNG import android.*
│   ├── model/           data class / enum (Pad, Lesson, Drum, Record…)
│   ├── repository/      interface
│   ├── usecase/         <Việc>UseCase (operator fun invoke), gom theo tính năng trong một file được
│   ├── audio/           interface player
│   └── <tính-năng>/     logic thuần: parser, chấm điểm, mixer, state machine  ← unit test nhiều nhất
├── data/
│   ├── local/           <App>Database (Room, version, autoMigrations)
│   ├── <tính-năng>/     <X>RepositoryImpl, local/<X>Entity, local/<X>Dao, <X>Mapper
│   └── audio/ sound/ record/ …   cài đặt kỹ thuật (SoundPool, MediaCodec, tải / giải nén)
└── feature/<màn>/       <Màn>Activity, <Màn>ViewModel, <Màn>Contract; view tuỳ biến; con: feature/<màn>/<màn con>/
```

Tên class: `<Màn>Activity`, `<Màn>ViewModel`, `<Màn>Contract.kt` (chứa `<Màn>UiState`, `<Màn>Intent`, `<Màn>Effect`),
`<X>Repository` / `<X>RepositoryImpl`, `<Việc>UseCase`, `<X>Entity`, `<X>Dao`, `<X>Mapper`, `<X>Controller` (logic dùng
chung nhiều màn), `<X>ScreenBinder` (phần View dùng chung).

### 10.3 Quy tắc MVI

- View chỉ: gửi `Intent` (`viewModel.onIntent(...)`) và vẽ theo `state` (`collectWhenStarted(viewModel.state, ::render)`).
- `render(state)` vẽ **toàn bộ** từ state, chạy lại nhiều lần không sai. Tránh set lại khi giá trị không đổi (so trước
  khi `setText` / `setImage` để không giật marquee / animation).
- `UiState`: data class bất biến, mọi trường có mặc định. Đổi bằng `setState { copy(...) }`.
- `Effect`: điều hướng, toast, animation một lần, xin quyền. Channel BUFFERED, nên effect phát lúc màn ở nền vẫn được
  giữ.
- Intent đặt tên theo **hành động người dùng** (`StyleClicked`, `SongPicked`), không theo việc phải làm.
- Màn rời foreground gửi `ScreenStopped` từ `onStop`.
- Logic dùng chung nhiều màn: `Controller` (class `@Inject`, `attach(viewModelScope)`, có `state` / `effects` riêng,
  ViewModel giữ instance) + `ScreenBinder` phía View (vd. REC: `RecordController` + `RecordScreenBinder`).
- Dữ liệu cần sống qua process death (bản nháp đang sửa) lưu trong `SavedStateHandle`.
- Kết quả trả về màn trước: `ActivityResultContract` riêng (`Contract()` lồng trong Activity).

### 10.4 Quy tắc an toàn (bắt buộc)

| Quy tắc | Lý do / lỗi đã gặp |
|---|---|
| SoundPool / MediaPlayer **không scope** trong DI: mỗi ViewModel một instance, release trong `onCleared`, hàm an toàn khi gọi sau release | Dùng chung singleton thì màn này release làm màn kia mất tiếng |
| `onStop`: dừng âm, tạm dừng nhạc, huỷ lượt chơi dở, đang thu thì Stop | Âm chạy ngầm khi về nền |
| Việc phải sống khi rời màn (tải gói dữ liệu) chạy trên `@ApplicationScope`, chặn chạy trùng | Rời màn huỷ tải giữa chừng |
| IO qua `@IoDispatcher`; **không** đọc file / prefs lớn / `MediaPlayer.prepare()` file lớn trên main thread | ANR |
| Thời gian đo bằng `MonotonicClock` inject | Test được; không lệch khi đổi giờ máy |
| Dữ liệu ngoài (JSON, MIDI, zip, prefs, dòng DB) đọc chịu lỗi: dòng hỏng bỏ qua + mặc định | Một bài hỏng làm sập cả danh sách |
| Ghi file: ghi `.part` rồi `renameTo`; thay bộ dữ liệu: staging → backup → swap, lỗi thì khôi phục | Tắt app giữa chừng để lại file dở |
| Giải nén zip: chỉ lấy tên file (chặn zip-slip), bỏ `__MACOSX` / file ẩn, giới hạn dung lượng, kiểm header | Zip rác / độc hại |
| Tải mạng: timeout, kiểm HTTP 2xx, kiểm dung lượng trống, đủ byte, huỷ được, phân loại lỗi | Báo lỗi rõ, "Thử lại" được |
| Chặn bấm đúp mở 2 màn (`openOnce`, reset ở `onResume`); toast dùng lại một instance | Mở chồng màn, toast xếp hàng |
| Room đổi schema: tăng version + `@AutoMigration(from, to)`, commit file schema mới | Mất dữ liệu khi cập nhật |
| Link / version dữ liệu tải về đặt ở `buildConfigField`, đè được bằng `-P` khi build debug | Đổi server không cần sửa code |
| **Một nguồn sự thật** cho ngưỡng / công thức: danh sách và màn kết quả dùng cùng hàm | T04: sao danh sách Drum (20/50/90) lệch sao màn kết quả (dùng hàm của Pad 26/50/90) |
| Code chỉ cho phát triển đặt ở `app/src/debug/` | Không lọt vào release |

### 10.5 Kế hoạch phase

- Chia theo tính năng, **mỗi phase chạy được độc lập trên máy**. Thứ tự: hạ tầng + màn chính → tính năng phụ thuộc →
  hoàn thiện (audio focus, theme, quảng cáo, dịch).
- T04: Phase 1 Pad + tải bộ sample · Phase 2 Customize · Phase 3 Lesson · Phase 4 REC + My records · Phase 4b Drum
  (Style, Volume, Add Song, Lesson trống) · Phase 5 hoàn thiện.
- Ghi vào PROGRESS mục "Việc tiếp theo", mỗi phase có checkbox việc con.
- Thẻ / nút trên Home dẫn tới phần chưa làm: hiện toast "Coming soon".

---

## 11. Giai đoạn 7: Làm một phase

### 11.1 Vòng làm việc

1. **Đọc lại**: PROGRESS (quyết định, việc tiếp theo), SPEC_NOTES của các màn, phần ANALYSIS của tính năng, node Figma.
2. **Lập danh sách việc** (TodoWrite / checklist) gồm cả test và cập nhật docs.
3. **Chốt điểm chưa rõ** (mục 14) trước khi code phần bị ảnh hưởng.
4. **Domain**: model, interface, logic thuần + **unit test** với số thật (app mẫu / spec). Test cả giá trị ngay dưới /
   bằng / ngay trên ngưỡng.
5. **Data**: Entity / Dao / migration, RepositoryImpl, mapper chịu lỗi, `@Binds` trong `BindingModule`.
6. **Presentation**: Contract → ViewModel → layout (mục 7) → Activity (`@AndroidEntryPoint`, khai báo manifest với
   `exported="false"`, `screenOrientation`), strings (en, + ngôn ngữ app đã có).
7. **Build + unit test** (mục 2).
8. **Chạy trên máy thật**: cài, đi đủ luồng, chụp màn so Figma, `check_crash.sh`.
9. **Test plan** (GĐ 8).
10. **Cập nhật PROGRESS** + `CLAUDE.md` / `BASE_GUIDE.md` nếu thêm quy tắc hoặc đổi màn base.
11. **Commit** phần vừa xong (mục 15.3). Hết phase thì **báo người dùng** (mục 15.2). Push chỉ khi được bảo.

### 11.2 Điều khiển máy test

- Chạm / vuốt / gõ / Back: `adb shell input ...`; máy chặn (MIUI) thì dùng AdbDriver (bản debug):
  ```bash
  "$ADB" shell am broadcast -a $PKG.debug.TAP --ei x 540 --ei y 1200
  "$ADB" shell am broadcast -a $PKG.debug.TAPS --es seq "136,1016,0;359,1016,400"   # chuỗi chạm có mốc ms
  "$ADB" shell am broadcast -a $PKG.debug.SWIPE --ei x1 900 --ei y1 1000 --ei x2 100 --ei y2 1000
  "$ADB" shell am broadcast -a $PKG.debug.TYPE --es text "abc"
  "$ADB" shell am broadcast -a $PKG.debug.BACK
  ```
- Toạ độ: `ui_dump.sh` → `bounds` theo `resource-id`. View tự vẽ (vd. bộ trống) thì đổi toạ độ design sang pixel
  theo scale của view.
- Thử server tải dữ liệu riêng: `python -m http.server 8765 --bind 127.0.0.1` trong thư mục file, `adb reverse tcp:8765
  tcp:8765`, build với `-PsoundPackUrl=http://127.0.0.1:8765/<file>` (debug cho phép http tới 127.0.0.1 qua
  `src/debug/res/xml/network_security_config.xml`).
- Xoá dữ liệu của **chính app** để thử lại: `run-as $PKG rm -rf files/<thư mục>`.
- Cài lỗi `INSUFFICIENT_STORAGE`: kiểm `df /data`. Có APK cũ trong `/data/local/tmp` thì **hỏi** rồi mới xoá. Không xoá
  dữ liệu app khác.
- **Không** đổi cài đặt máy (`wm size`, density, font scale, developer options, "Don't keep activities").

### 11.3 Định nghĩa "xong" của một phase

- [ ] Mọi việc con của phase trong PROGRESS đã tick hoặc ghi rõ lý do để lại.
- [ ] Logic thuần có unit test; `testDebugUnitTest` pass.
- [ ] Chạy trên máy thật đủ luồng, không crash (`check_crash.sh` sạch).
- [ ] UI khớp Figma, không cắt chữ (mọi ngôn ngữ app có).
- [ ] Test plan của tính năng đã chạy, ít nhất bộ smoke P0 (GĐ 8).
- [ ] PROGRESS cập nhật: đã làm, khác app mẫu ở đâu, chưa test gì, lưu ý.

---

## 12. Giai đoạn 8: Test

Theo `docs/testing/TEST_FORM.md`:
1. Đọc code tính năng → lập các bảng phân tích (Intent, Effect, State, tài nguyên, dữ liệu, đầu vào, ngưỡng, song song,
   rủi ro, khác biệt đã chốt).
2. Viết `docs/testing/<tính-năng>.md` (mẫu ở mục 10 của TEST_FORM; ví dụ đầy đủ: `docs/testing/examples/drum.md`).
3. Chạy unit test → bộ smoke P0 → các nhóm còn lại. Ghi KQ từng ca, chụp bằng chứng vào `build/test-evidence/`.
4. Lỗi tìm được: ghi bảng lỗi (mức S1–S4, `file:dòng`), sửa, chạy lại ca lỗi + nhóm C + M + smoke.
5. Thêm báo cáo lượt test vào cuối plan và một dòng tóm tắt vào PROGRESS.

---

## 13. Lỗi đã gặp và cách tránh

| Tình huống | Triệu chứng | Cách làm đúng |
|---|---|---|
| Đổi locale trước khi mở màn kế | Màn bị tạo lại giữa chừng, kẹt ở Language | Mở màn kế rồi mới đổi locale |
| Hilt với BaseActivity | Lỗi build "cannot override final" | Bỏ `final` ở `onCreate` / `onDestroy` |
| Ảnh pad bo góc | Pad màu nhạt lộ góc tối | Ảnh tràn màu ra góc, bo bằng outline |
| Font thiếu glyph tiếng Việt | Chữ vi lẫn font hệ thống | Style cùng tên trong `values-vi` dùng font có glyph vi |
| Dialog trên màn immersive (ngang) | Thanh hệ thống hiện lại khi mở dialog | Dialog copy cờ ẩn thanh của màn gọi |
| Link `*.r2.dev` | Máy test không phân giải DNS, tải lỗi | Thử bằng server local (11.2); báo người dùng đổi link |
| `JAVA_HOME` chưa set | `./gradlew` báo không tìm thấy java | `bash tools/dev_env.sh` rồi `source .dev-env` |
| Git Bash đổi đường dẫn | `adb shell df /data` ra `C:/Program Files/Git/data` | `export MSYS_NO_PATHCONV=1` |
| Máy test gần đầy (< ~600 MB trống) | `INSTALL_FAILED_INSUFFICIENT_STORAGE` với APK có FFmpegKit (66–80 MB) | Gỡ bản cũ của chính app đang test rồi cài; APK app mẫu: bỏ `lib/<ABI khác>` trong zip, `zipalign -p 4`, ký `apksigner.jar` bằng `~/.android/debug.keystore` (P19: 80 → 51 MB). Không xoá dữ liệu app khác khi chưa hỏi |
| Bảng "AdMob native ad validator" (test ad) | Che nút gần native, tap rơi vào bảng | Bấm Dismiss của bảng trước khi tap nút bên dưới |
| Máy MIUI | `adb shell input` bị chặn | AdbDriver broadcast |
| Repo mới clone / copy chưa có `local.properties` | Gradle: "SDK location not found" | `bash tools/dev_env.sh` + `source .dev-env` (đặt `ANDROID_HOME`) |
| Máy test đầy bộ nhớ | `INSTALL_FAILED_INSUFFICIENT_STORAGE` | Hỏi người dùng dọn (APK cũ trong `/data/local/tmp`) |
| Zip của macOS | Lẫn file `__MACOSX/._01.wav` | Lọc theo mẫu tên, bỏ file ẩn |
| Hai nơi tự tính cùng một ngưỡng | Sao danh sách ≠ sao kết quả | Một hàm dùng chung trong domain |
| `MediaPlayer.prepare()` trên main | Đứng UI với file lớn | `prepareAsync` hoặc chạy trên IO |
| Bấm nhanh hai lần | Mở hai màn giống nhau | `openOnce` + reset ở `onResume` |

Gặp lỗi mới đáng nhớ thì **thêm dòng vào bảng này** (file này dùng chung cho các app sau).

---

## 14. Khi nào hỏi, khi nào tự quyết

**Phải hỏi** (AskUserQuestion, gộp câu, mỗi câu có phương án đề xuất và hệ quả):
- Spec, Figma và app mẫu **mâu thuẫn**, hoặc spec không nói mà cách làm ảnh hưởng trải nghiệm / dữ liệu.
- Cần thông tin chỉ người dùng có: ID quảng cáo, link server, tài khoản, file thật, APK, node Figma không tìm thấy.
- Việc khó hoàn tác hoặc đụng ra ngoài: xoá dữ liệu trên máy test, đổi cài đặt máy, push, publish.
- Muốn làm khác một quyết định đã chốt, hoặc đổi phạm vi.
- Thêm thư viện lớn / có giấy phép lạ (vd. bộ mã hoá MP3 LAME), hoặc thêm quyền nhạy cảm (micro, vị trí).
- Hai cách làm đều hợp lý mà khác nhau rõ về trải nghiệm.

**Tự quyết** (ghi lý do vào PROGRESS hoặc KDoc):
- Chi tiết kỹ thuật trong kiến trúc đã chốt: tên class, chia file, cấu trúc dữ liệu, thuật toán.
- Xử lý lỗi an toàn (bỏ dòng hỏng, toast, không crash, Thử lại).
- Trạng thái Figma không vẽ (loading / rỗng / lỗi): làm theo style các màn đã có.
- Sửa lỗi tìm được khi test, miễn không đổi hành vi đã chốt.
- Thứ tự việc trong một phase.

**Không làm khi chưa được yêu cầu:** push; thêm ngôn ngữ; gắn quảng cáo ở màn mới; thêm màn / placement riêng của app vào
base; đổi cài đặt máy test.

---

## 15. Báo cáo

### 15.1 PROGRESS.md

- Là "bộ nhớ" giữa các phiên: phiên sau chỉ đọc **mục 0 "Trạng thái nhanh"** phải biết đang ở đâu, làm gì tiếp.
  Cách viết cho ít token: `docs/CODING_RULES.md` mục 8 (bắt buộc).
- Cập nhật **sau mỗi phần** (không đợi hết phase), kể cả khi dở: ghi dở ở đâu, vì sao, bước tiếp.
- Mục "Đã làm" ghi **hành vi và quyết định**, không chép code: cái gì chạy được, theo app mẫu ở đâu (class), khác app
  mẫu ở đâu và vì sao, file / class chính, unit test nào, đã chạy trên máy nào, **chưa test gì**.
- Mục "Lưu ý" luôn có: nội dung tạm lấy từ app mẫu (phải thay), ID / link tạm, lỗi đã biết, phần chưa chạy trên máy.
- Mục "Việc tiếp theo": phase hiện tại đánh dấu "(đang làm)", việc con có checkbox; xong thì ✅ + "(đã chạy trên máy)".

### 15.2 Báo người dùng cuối mỗi phần

Ngắn, tiếng Việt, theo thứ tự:
1. **Đã làm gì** (theo hành vi người dùng thấy, không liệt kê file dài dòng);
2. **Đã kiểm thế nào** (bao nhiêu unit test, chạy máy nào, ca nào);
3. **Chưa làm / chưa kiểm / rủi ro** (nói thẳng, có lý do);
4. **Cần người dùng quyết** (nếu có);
5. **Bước tiếp theo** đề xuất.

### 15.3 Commit (tự commit mỗi phần, push khi được bảo)

- Message tiếng Việt, một dòng mô tả việc + dòng attribution theo hướng dẫn của hệ thống.
- Commit sau **mỗi phần** xong (build + unit test pass): mỗi giai đoạn, mỗi màn / tính năng con; đổi package một commit
  riêng. Push chỉ khi người dùng bảo.
- Không commit `.idea/`, `build/`, `local.properties`, ảnh test-evidence.
- Commit kèm file schema Room mới (`app/schemas/`).

Ví dụ message đã dùng: `Khởi tạo Beat Maker từ base của T03 Shimeji Pets` · `Đổi package name thành
com.wavez.music.beatmaker` · `Import màu, font và kiểu chữ từ Figma T04 - Design` · `Dựng UI flow Splash, Language,
Onboarding theo Figma T04` · `Tạm bỏ quảng cáo ở Splash, Language, Onboarding`.

---

## 16. Mẫu

### 16.1 `docs/<app>/PROGRESS.md`

```markdown
# <App>: tiến độ và việc tiếp theo

File này để phiên làm việc sau đọc trước khi code tiếp. **Cập nhật file này mỗi khi xong một phần.**

- Figma: file `<fileKey>` (<tên file>). Spec: section `<node>`. Màn: Home `<node>`, <Màn> `<node>`...
- App mẫu: `<package>` v<x>. Bản decompile `~/Downloads/Decompile-<App>`, phân tích `<APP>_ANALYSIS.md`.
- Base: copy từ <app base>. Ghi chú spec: `docs/<app>/SPEC_NOTES.md`.

## 0. Trạng thái nhanh (cập nhật <yyyy-mm-dd>)
- Phase: <tên> - <đang làm / đang test / xong>
- Đang dở: <việc> - <file / class> - <còn thiếu gì>
- Bước tiếp theo: <1–3 việc cụ thể>
- Chờ user: <câu hỏi / thông tin, hoặc "không">
- Lần build / test gần nhất: <ngày> - unit <n> pass - máy <model>: <đã chạy> - chưa chạy: <...>
- Lỗi đang mở: <ID + file:dòng, hoặc "không">

---

## 1. Quyết định đã chốt với user
| Chủ đề | Quyết định |
|---|---|
| Kiến trúc | Clean Architecture + MVVM/MVI; Hilt <ver> (+ KSP <ver>); Room <ver> (schema `app/schemas/`) |
| Quảng cáo | Chưa gắn ở <màn>. Hạ tầng `core/ads` giữ nguyên |
| Nội dung tạm | <sample / dữ liệu> lấy từ app mẫu, thay trước phát hành |
| Giao diện vs app mẫu | Luồng / chức năng / core theo app mẫu, hình theo Figma |
| <điểm đã hỏi> | <đã chọn> - chốt <dd/mm/yyyy> |

---

## 2. Đã làm
### 2.1 Hạ tầng
### 2.2 <Tính năng 1>
- Màn / class chính, hành vi, theo app mẫu ở đâu, khác ở đâu.
### 2.x Test
- Unit test: <tên test (số ca)>. Đã chạy máy <model>: <luồng / ca>. Chưa test: ...

---

## 3. Lưu ý / vấn đề đã biết
- Nội dung lấy từ app mẫu: <danh sách> - **phải thay trước phát hành**.
- ID / link tạm: ...
- Chưa chạy trên máy: ...

---

## 4. Việc tiếp theo (theo thứ tự)
### Phase 1: <tên> ✅ (đã chạy trên máy)
- [x] ...
### Phase 2: <tên> (đang làm - <dở ở đâu>)
- [ ] ...

---

## 5. Cấu trúc thư mục (quy ước)
(cây thư mục mục 10.2 của playbook, chỉ ghi phần riêng của app)
```

### 16.2 `docs/<app>/SPEC_NOTES.md`

```markdown
# Ghi chú spec <App>
Nguồn: Figma `<fileKey>` section `<node>` (đọc ngày <dd/mm/yyyy>).

## <Màn> (`<node>`)
- Vào từ: ... · Ra tới: ...
- Trạng thái: mặc định / loading / rỗng / lỗi / ...
- Thao tác → kết quả:
  | Thao tác | Kết quả |
  |---|---|
- Quy tắc / số liệu: ...
- Khác app mẫu: ...
- Câu hỏi mở: ... → (đã chốt <ngày>: ...)
```

### 16.3 `<APP>_ANALYSIS.md` (gốc thư mục decompile)

```markdown
# Phân tích <tính năng> của app mẫu
App: <tên> (`<package>`, v<x>). Phạm vi: <tính năng>. Không gồm: <...>.
> Code bị R8 làm rối; đường dẫn tính từ `app/src/main/java/`. Method JADX không dịch được ghi ⚠️.

## Mục lục
## 1. Tổng quan kiến trúc        (sơ đồ khối UI → engine → dữ liệu)
## 2. Gọi mạng                   (Remote Config key, URL, luồng tải, xử lý lỗi)
## 3. Lưu local                  (3.1 Room: bảng/cột · 3.2 SharedPreferences: key · 3.3 File · 3.4 Có sẵn trong APK)
## 4..n. <Tính năng>             (luồng, dữ liệu vào/ra, công thức + hằng số thật, xử lý lỗi, class/method)
## n+1. Bản đồ file quan trọng   (class bị đổi tên → vai trò)
## n+2. ⚠️ Method không decompile được và cách suy ra
## n+3. Tóm tắt: nên giữ / nên làm khác
```

### 16.4 Phần app trong `CLAUDE.md`

```markdown
## <App> (<MÃ>)

- **Đọc `docs/<app>/PROGRESS.md` trước**: đã làm gì, quyết định đã chốt, việc tiếp theo. Cập nhật file đó sau mỗi phần.
- Quy trình làm app / phase: `docs/APP_PLAYBOOK.md`. Test: `docs/testing/TEST_FORM.md` (+ plan từng tính năng).
- Kiến trúc: Clean Architecture (`domain/` thuần Kotlin, `data/`, `feature/` presentation) + MVVM/MVI
  (`core/mvi/MviViewModel`, Contract State/Intent/Effect). DI bằng Hilt (`di/`), lưu local bằng Room (`data/local`,
  schema ở `app/schemas/`).
- <config đặc thù: link dữ liệu trong build.gradle.kts, ...>
```

### 16.5 Contract + ViewModel + Activity

```kotlin
// feature/x/XContract.kt
data class XUiState(val loading: Boolean = true, val items: List<Item> = emptyList(), val error: Boolean = false)

sealed interface XIntent {
    data class ItemClicked(val id: Long) : XIntent
    data object RetryClicked : XIntent
    data object ScreenStopped : XIntent
}

sealed interface XEffect {
    data class OpenDetail(val id: Long) : XEffect
    data class Message(@StringRes val text: Int) : XEffect
}

// feature/x/XViewModel.kt
@HiltViewModel
class XViewModel @Inject constructor(private val observeItems: ObserveItemsUseCase) :
    MviViewModel<XUiState, XIntent, XEffect>(XUiState()) {
    init {
        viewModelScope.launch { observeItems().collect { setState { copy(loading = false, items = it) } } }
    }
    override fun handleIntent(intent: XIntent) {
        when (intent) {
            is XIntent.ItemClicked -> sendEffect(XEffect.OpenDetail(intent.id))
            XIntent.RetryClicked -> Unit
            XIntent.ScreenStopped -> Unit
        }
    }
}

// feature/x/XActivity.kt
@AndroidEntryPoint
class XActivity : BaseActivity<ActivityXBinding>() {
    private val viewModel: XViewModel by viewModels()
    override fun inflateBinding(inflater: LayoutInflater) = ActivityXBinding.inflate(inflater)
    override fun initListeners() { binding.retry.setOnClickListener { viewModel.onIntent(XIntent.RetryClicked) } }
    override fun observeData() {
        collectWhenStarted(viewModel.state, ::render)
        collectWhenStarted(viewModel.effects) { effect -> when (effect) { ... } }
    }
    private fun render(state: XUiState) { binding.progress.isVisible = state.loading }
    override fun onStop() { viewModel.onIntent(XIntent.ScreenStopped); super.onStop() }
}
```

---

## 17. Trước khi phát hành

- [ ] Thay toàn bộ nội dung lấy từ app mẫu (âm thanh, bài học, MIDI, ảnh). Đổi link tải dữ liệu sang server của mình,
      tăng version dữ liệu.
- [ ] `google-services.json`, AdMob app ID, `ad_ids`, Adjust token, Billing product ID là bản thật.
- [ ] Quảng cáo gắn theo yêu cầu (placement, sheet, README remote config đồng bộ).
- [ ] `versionCode` tăng. Cấu hình theo bản dùng Firebase Condition "App build ≥ N" (không tạo key theo version).
- [ ] Giấy phép font / thư viện có trong `tools/` hoặc `docs/licenses/`.
- [ ] Build release (minify bật) chạy được; mọi test plan trong `docs/testing/` đạt exit criteria trên bản release.
- [ ] PROGRESS mục "Lưu ý" không còn mục chặn phát hành.
