# Quy tắc code (agent đọc trước khi viết code)

Mục tiêu: code **đúng, ít, thẳng**. Không rườm rà, không vòng vo, không làm thừa. Quy trình chung ở `docs/APP_PLAYBOOK.md`.
Kiến trúc ở playbook mục 10.

---

## 1. Trước khi viết một dòng code

1. **Hiểu đúng yêu cầu.** Làm đúng cái được yêu cầu, không thêm tính năng, không "tiện tay" refactor chỗ khác. Thấy
   chỗ khác đáng sửa thì ghi vào PROGRESS "Lưu ý" hoặc báo người dùng.
2. **Tìm cái đã có rồi dùng lại.** `Grep` tên class, hàm, string, drawable trước khi tạo mới. Base có sẵn:
   `BaseActivity` / `BaseFragment` / `BaseDialogFragment` (hook), `BaseRecyclerAdapter`, helper quảng cáo,
   `AppPreferences`, `AppLanguage`, `MviViewModel`, `collectWhenStarted`. App thêm gì dùng chung (toast, quyền, đồng hồ,
   kiểm mạng…) thì đặt trong `core/` và grep trước khi viết lại. Màu / chữ dùng token Figma.
3. **Đọc đúng chỗ cần đọc.** Grep ra vị trí rồi đọc đoạn đó (offset / limit). Không đọc cả file lớn khi chỉ cần một
   hàm. Không đọc lại file vừa sửa.
4. **Xác định lớp sẽ sửa** (domain / data / feature) và **test nào chứng minh nó đúng**.

## 2. Nguyên tắc viết

| Làm | Không làm |
|---|---|
| Cách đơn giản nhất mà đúng | Interface / abstract / factory khi chỉ có một cài đặt và không cần test giả |
| Một hàm làm một việc, tên nói rõ việc đó | Hàm 100 dòng, hoặc tách vụn thành 10 hàm 2 dòng chỉ gọi một lần |
| Hằng số có tên (`const val PERFECT_MS = 1_000L`) đặt cạnh nơi dùng | Số ma thuật rải rác; file `Constants.kt` chung chung |
| Một nguồn sự thật cho mỗi quy tắc (ngưỡng, công thức, format) | Hai nơi tự tính cùng một thứ (lỗi sao Drum: danh sách 20/50/90, kết quả 26/50/90) |
| `try/catch` ở **biên**: IO, parse dữ liệu ngoài, SDK hệ thống | `try/catch` bọc cả hàm "cho chắc"; nuốt lỗi không log, không báo |
| Bắt `CancellationException` thì ném lại | Bắt `Exception` làm mất huỷ coroutine |
| Trả kết quả rõ (`Boolean`, `sealed`, `null` có nghĩa) | Cờ toàn cục, biến `isXxx` trùng lặp với state |
| Xoá code chết, import thừa, log debug trước khi báo xong | Để lại code comment-out, `println`, TODO không chủ |
| Comment / KDoc ngắn nói **vì sao**, theo app mẫu ở đâu, khác app mẫu ở đâu | Comment lặp lại code ("// set text") |
| Tham số / cờ chỉ thêm khi có chỗ dùng ngay | Tham số "phòng khi sau này cần" |

## 3. Kotlin

- `val` mặc định; `var` chỉ khi thật sự đổi. Collection bất biến ở API công khai.
- `data class` cho dữ liệu, `enum` cho tập cố định, `sealed interface` cho Intent / Effect / kết quả nhiều nhánh;
  `when` phải **exhaustive** (không `else` cho sealed).
- Không dùng `!!`. `lateinit` chỉ cho binding, DI, scope `attach`.
- Null: `?.let`, `?:` với giá trị mặc định có nghĩa, hoặc `return` sớm.
- Extension function cho tiện ích nhỏ gắn với một kiểu (vd. `View.padForSystemBars()`), không làm "Utils" object.
- Coroutines: `viewModelScope` trong ViewModel; `@ApplicationScope` cho việc sống qua màn; **không** `GlobalScope`,
  không `runBlocking` ở main. `withContext(io)` đặt **trong repository / data**, ViewModel không tự đổi dispatcher.
- Flow: dữ liệu theo dõi được (DB, prefs) trả `Flow` / `StateFlow`; việc một lần trả `suspend fun`.
- `StateFlow.update { }` để đổi atomic; không `value = value.copy()` khi có thể chạy song song.

## 4. Theo lớp

| Lớp | Được | Không được |
|---|---|---|
| `domain/` | Kotlin thuần, model, interface, logic thuần, use case | `import android.*`, Context, Room annotation |
| `data/` | Room, file, mạng, SDK; map Entity ↔ model domain; chịu lỗi dữ liệu xấu | Trả Entity ra ngoài; logic UI |
| ViewModel | Gọi use case / repository / controller; giữ state; gửi effect | `Context` (trừ khi `@ApplicationContext` thật cần), `View`, `Activity`, string resource đã format |
| Activity / Fragment | Inflate, gắn listener → Intent, `render(state)`, xử lý effect (điều hướng, toast, quyền) | Logic nghiệp vụ, gọi repository, giữ state riêng ngoài cờ UI tạm (`openOnce`) |
| Custom View | Vẽ, đo, nhận chạm, báo callback | Gọi ViewModel / repository |

- Use case chỉ tạo khi có logic hoặc dùng ở nhiều chỗ. Chỉ chuyển tiếp một hàm repository thì ViewModel gọi repository
  thẳng (dự án đã có use case dạng `operator fun invoke` cho record / lesson: giữ cách đó cho nhất quán trong tính năng
  đó).
- Room: entity tên `<X>Entity`, bảng snake_case; đổi schema = tăng version + `AutoMigration`.

## 5. Android

- Màn mới: kế thừa `BaseActivity`, `@AndroidEntryPoint`, khai báo manifest (`exported="false"`, `screenOrientation`).
  Logic chỉ trong các hook `initConfig` / `initViews` / `initListeners` / `observeData` / `releaseResources`.
- Mọi chữ trong `strings.xml` **en + vi**. Mọi màu là `@color/` token. Kích thước theo tỉ lệ khi phần tử co theo màn.
- Tài nguyên phải release: player trong `onCleared`; listener gán `null`; `onStop` dừng âm thanh.
- Chặn bấm đúp mở màn; toast dùng lại một instance (hoặc `AppToast`).
- Quyền: kiểm theo API qua `StorageAccess`; từ chối hẳn → dẫn tới Cài đặt app.
- Không IO nặng trên main (`MediaPlayer.prepare()` file lớn, đọc file, prefs lớn lần đầu).
- Quảng cáo chỉ qua helper base (`loadNative`, `loadBanner`, `showInterstitial`, `showReward`), không gọi SDK trực tiếp.

## 6. Sửa lỗi

1. **Tái hiện** (bước cụ thể hoặc unit test) trước khi sửa.
2. Tìm **nguyên nhân gốc**, sửa ở đó. Không vá triệu chứng (vd. thêm `delay`, thêm `try/catch` quanh chỗ crash).
3. Thêm unit test bắt đúng lỗi nếu là logic thuần.
4. Grep xem lỗi cùng kiểu có ở chỗ khác không, nếu có thì sửa cùng lúc.
5. Ghi lỗi mới đáng nhớ vào bảng mục 13 của playbook.

## 7. Tự kiểm trước khi báo "xong"

- [ ] `source .dev-env && ./gradlew :app:testDebugUnitTest :app:assembleDebug` pass (không có warning mới do mình).
- [ ] Đọc lại diff (`git diff --stat` + diff các file chính): không có code chết, log debug, file sửa nhầm, chữ / màu
      hard-code.
- [ ] Chạy trên máy nếu đụng UI / âm thanh / vòng đời; `check_crash.sh` sạch.
- [ ] Cập nhật ghi chú theo mục 8.
- [ ] Báo người dùng đúng sự thật: cái gì đã chạy, cái gì chưa kiểm.

## 8. Ghi chú cho phiên sau (ít token, đọc là hiểu)

Phiên sau chỉ được đọc **ít nhất có thể** mà vẫn biết làm tiếp. Vì vậy:

### 8.1 PROGRESS.md: mục "0. Trạng thái nhanh" ở đầu file (≤ 12 dòng)

Phiên sau đọc mục này trước. Chỉ khi cần mới đọc các mục dưới.

```markdown
## 0. Trạng thái nhanh (cập nhật <yyyy-mm-dd>)
- Phase: <tên phase> - <đang làm / đang test / xong>
- Đang dở: <việc> - <file:dòng hoặc class> - <còn thiếu gì>
- Bước tiếp theo: <1–3 việc cụ thể, theo thứ tự>
- Chờ user: <câu hỏi / thông tin đang chờ, hoặc "không">
- Lần build / test gần nhất: <ngày> - unit <n> pass - máy <model>: <đã chạy gì> - chưa chạy: <...>
- Lỗi đang mở: <ID ngắn + file:dòng, hoặc "không">
```

### 8.2 Viết các mục còn lại của PROGRESS

- Mỗi ý **một dòng**, bắt đầu bằng cái gì / ở đâu: `` `DrumLessonController`: đếm 3-2-1 → demo theo MIDI → Practice ``.
- Ghi **đường dẫn file / class** thay vì giải thích code. Người đọc cần chi tiết thì mở file.
- Ghi **quyết định + lý do ngắn**, không ghi quá trình mò mẫm.
- Số liệu ghi số thật (ngưỡng, version, kích thước).
- Phase xong: nén chi tiết của phase còn ≤ 8 dòng (hành vi chính, khác app mẫu, đã test gì). Chi tiết dài (bảng
  công thức, phân tích) chuyển vào KDoc của class hoặc `ANALYSIS`, PROGRESS chỉ trỏ tới.
- Giữ PROGRESS **≤ 250 dòng**. Dài hơn thì nén các phase đã xong.
- Không lặp lại nội dung đã có trong playbook / CODING_RULES / BASE_GUIDE.

### 8.3 Ghi chú trong code

- KDoc đầu mỗi class chính: **vai trò** (1 dòng), **luồng** nếu có (1–4 dòng), **theo app mẫu ở đâu / khác gì**. Phiên
  sau đọc KDoc là hiểu class, không phải đọc hết thân hàm.
- Chỗ code làm điều không hiển nhiên (thứ tự gọi, giá trị lạ, workaround) có 1 dòng "vì sao".
- Không viết lịch sử thay đổi trong comment (đã có git).

### 8.4 Khi nào ghi

- Xong mỗi **phần** (không đợi hết phase): cập nhật "Trạng thái nhanh" + dòng tương ứng ở "Đã làm".
- Người dùng chốt quyết định: thêm dòng vào "Quyết định đã chốt" ngay.
- Trước khi dừng giữa chừng (hết lượt, chờ user): cập nhật "Trạng thái nhanh" để phiên sau tiếp đúng chỗ.

## 9. Làm việc tiết kiệm token

- Grep trước, đọc sau; đọc theo đoạn. Gộp nhiều lệnh độc lập vào một lần gọi.
- Lệnh có output dài: lọc bằng `grep` / `tail` / `head`; gradle chạy `-q` và chỉ in phần lỗi.
- Không đọc lại file vừa ghi; không in lại cả file trong câu trả lời.
- Câu trả lời cho người dùng: kết quả + chưa làm + cần quyết. Không kể lại từng bước đã làm.
- Không hỏi lại điều đã có trong PROGRESS / playbook.
