# Test plan: Drum (chơi trống)

Viết theo [TEST_FORM.md](TEST_FORM.md).

## 0. Thông tin

- **Phạm vi (code):**
  - Màn: `feature/drum/` gồm `DrumActivity`, `DrumViewModel`, `DrumContract`, `DrumKitView`, `DrumSkin`, `DrumScreens`;
    `style/` (DrumStyleActivity, Adapter, ViewModel); `volume/`; `song/` (SongPickerActivity, ViewModel); `lesson/`
    (DrumLessonListActivity, DrumLessonController, DrumLessonResultActivity).
  - REC dùng chung khi chạy trên Drum: `feature/record/RecordController`, `RecordScreenBinder`, `RecordDialogs`,
    `RecordActions`.
  - Domain / data: `domain/drumlesson/` (MidiDrumParser, DrumLessonPractice), `domain/model/Drum.kt`,
    `domain/record/` (RecordingSession, BeatMixer), `data/drum/`, `data/drumlesson/`, `data/song/`,
    `data/audio/` (SoundPoolPadPlayer, MediaRecordPlayer), `data/record/` (SongBackingReader, StreamingAudioDecoder).
- **Ngoài phạm vi:** Metronome (chưa làm, chỉ kiểm toast); màn My Records (plan riêng; ở đây chỉ kiểm việc bản thu từ
  Drum xuất hiện và phát được); khoá Style / Lesson bằng quảng cáo thưởng (chưa gắn).
- **Đối chiếu:** `docs/beat-maker/PROGRESS.md` mục 1 (quyết định đã chốt) và Phase 4b; app mẫu `DrumSetActivity`,
  `DrumThemesActivity`, `DrumSettingActivity`, `ImportGuitarActivity`; Figma T04 Drum `10078:12891`, Volume `10085:37414`.
- **Máy / môi trường:**

  | Máy | API | Màn | Trạng thái |
  |---|---|---|---|
  | Realme RMX3231 (ColorOS / Realme UI) | 30 | 720×1600, 320 dpi → ngang 800×360 dp, tai thỏ giọt nước | chính |
  | Emulator API 24 (minSdk) | 24 | 360dp | ⏭ nếu chưa có AVD |
  | Emulator API 33+ (quyền READ_MEDIA_AUDIO) | 33+ | — | ⏭ nếu chưa có AVD |
  | Máy Android ≤ 9 (quyền WRITE_EXTERNAL_STORAGE khi Download) | ≤ 28 | — | ⏭ |
- **Dữ liệu chuẩn bị:**
  - bản debug có `AdbDriver`;
  - trên máy có ≥ 3 file nhạc ≥ 10 s: một file tên dài (> 60 ký tự), một file lớn (> 8 MB), một file ngắn < 10 s (để kiểm
    bị lọc);
  - một file `.mp3` rác (đổi đuôi từ file text) để kiểm C-07;
  - ghi lại điểm Lesson hiện có trong `shared_prefs/drum_lessons.xml` trước khi test.

---

## 1. Phân tích

### 1.1 Sơ đồ màn

```
Home ──thẻ DRUM──▶ DrumActivity (sensorLandscape, immersive)
   DrumActivity ──Lesson──▶ DrumLessonListActivity ──chọn bài (ActivityResult)──▶ DrumActivity: Lesson chạy tại chỗ
   DrumActivity ──xong bài──▶ DrumLessonResultActivity
        Result ──Replay / Next──▶ DrumActivity (CLEAR_TOP + SINGLE_TOP → onNewIntent, extra drum_lesson_number)
        Result ──Home──▶ MainActivity     Result ──Back──▶ DrumActivity
   DrumActivity ──Style──▶ DrumStyleActivity ──Apply / Back──▶ DrumActivity
   DrumActivity ──Volume──▶ DrumVolumeActivity ──Apply / Back──▶ DrumActivity
   DrumActivity ──Add Song──▶ [xin quyền] ──▶ SongPickerActivity (ActivityResult) ──▶ DrumActivity
                              └ từ chối hẳn ──▶ Cài đặt app
   DrumActivity ──REC──▶ (Stop) RecordNameDialog ──Save──▶ RecordSavedDialog ──My records──▶ MyRecordsActivity
                                                               ├ Download (Android ≤ 9: xin quyền ghi) ├ Share
```

### 1.2 Intent → ca (`DrumIntent`)

| Intent | Xử lý trong `DrumViewModel.handleIntent` | Ca |
|---|---|---|
| `Hit(part)` | phát âm theo gain của mặt; ghi REC; chuyển cho Lesson; âm chưa sẵn sàng → toast | F-02..F-05, F-07, C-01, S-05 |
| `RecordClicked` | `recorder.toggle()` | F-60..F-62, C-03 |
| `LessonClicked` | `openScreen` (bị chặn khi đang thu) | F-30, S-01 |
| `StyleClicked` / `VolumeClicked` | `openScreen` (stopAll, pause nhạc, chặn khi đang thu) | F-10, F-20, S-01, S-04 |
| `AddSongClicked` | chặn khi đang thu; pause nhạc; `PickSong` | F-50, S-01 |
| `SongPicked(song)` | stop bài cũ, `songStarted = false`, chưa tự phát | F-52, F-56 |
| `SongPlayClicked` | phát / tạm dừng / resume / phát lại; lỗi → bỏ bài | F-53..F-55, C-06, C-07 |
| `LessonPicked(n)` | `lesson.start(n)` (huỷ bài cũ) | F-31, S-07 |
| `MetronomeClicked` | toast Coming soon (chặn khi đang thu) | F-08, S-01 |
| `ToolbarVisible(v)` | ẩn / hiện thanh | F-06 |
| `BackPressed` | đang thu → Stop; Lesson đang chạy → huỷ; còn lại → Close | F-09, S-02, S-08 |
| `RetrySounds` | nạp lại âm | C-11 |
| `Record(action)` | Save / Cancel / Download / Share / My records / quyền | F-60..F-69 |
| `ScreenStopped` | stopAll, pause nhạc, huỷ Lesson, đang thu thì Stop | L-01..L-04 |

### 1.3 Effect → ca (`DrumEffect`, `RecordEffect`)

| Effect | Thấy được ở đâu | Ca |
|---|---|---|
| `ShowComingSoon` | toast "Coming soon" | F-08 |
| `OpenLessonList` / `OpenStyle` / `OpenVolume` | mở màn (chặn mở 2 lần bằng `openOnce`) | F-30, F-10, F-20, C-02 |
| `AnimateHit` / `ShakePart` | mặt nảy (demo) / rung ngang (sai mặt) | F-32, F-34 |
| `OpenLessonResult` | mở màn kết quả | F-33 |
| `LessonLoadFailed` | toast "không tải được bài" | A-07 (không tới được bằng UI) |
| `PickSong` | hộp xin quyền hoặc mở Choose music | F-50, Q-01..Q-04 |
| `SongPlayFailed` | toast lỗi phát, bỏ bài | C-06, C-07 |
| `ShowSoundNotLoaded` | toast khi chạm lúc âm chưa sẵn sàng | F-07 |
| `Close` | đóng Drum | F-09 |
| `RecordEffect.Message / Share / RequestPermission / OpenMyRecords` | toast / share sheet / xin quyền / mở My records | F-64..F-68 |

### 1.4 State / trạng thái hiển thị → ca

| Trạng thái | Hiển thị | Ca |
|---|---|---|
| `sound = LOADING` | bộ trống mờ 50%, hộp "đang chuẩn bị" + vòng quay | F-01, U-03 |
| `sound = FAILED` | hộp lỗi + nút Thử lại | C-11, U-03 |
| `toolbarVisible = false` | thanh ẩn, nút mắt hiện | F-06, U-02 |
| `song != null`, `songPlaying` | nút play / pause + tên bài chạy chữ | F-52..F-55, U-05 |
| Lesson `Countdown` / `Demo` / `Practice` | lớp đếm ngược, nhãn trạng thái, mặt nhấp nháy, nút Lesson ghi "Lesson N" | F-31, U-04 |
| `recording` | nút đỏ mm:ss, 5 nút mở màn mờ | F-60, S-01, U-06 |
| `nameDialog` / `savedRecordId` / `exportProgress` | hộp thoại Save / Saved / tiến trình | F-61..F-67, U-07 |
| Danh sách Lesson: chưa chơi / đã chơi | Learn / Replay, sao, % | F-30, F-40 |
| Choose music: loading / rỗng / có bài / đang nghe thử | | F-51, C-08, U-09 |

### 1.5 Tài nguyên sống lâu → ca

| Tài nguyên | Tạo / huỷ | Ca |
|---|---|---|
| `SoundPoolPadPlayer` (12 âm) của DrumViewModel | `load` trong init, `release` ở `onCleared`; `stopAll` ở `onStop` | M-01, M-03, C-09 |
| `MediaRecordPlayer` (nhạc nền) của DrumViewModel | `play` (prepare **đồng bộ** trên main), `release` ở `onCleared`, listener = null | M-03, P-03, L-02 |
| `MediaRecordPlayer` của SongPickerViewModel | nghe thử; stop ở `ScreenPaused`; release ở `onCleared` | M-02, F-51 |
| Job Lesson (`DrumLessonController.job`) | `start` / `cancel` (ScreenStopped, Back, onCleared) | L-04, S-07, M-04 |
| Job timer REC (200 ms) | `start` / `stop` / `release` | L-03, M-01 |
| `postInvalidateOnAnimation` của `DrumKitView` | lặp khi có `target` / đang nảy / đang rung | P-02, L-04 |
| Toast dùng lại, Vibrator | `releaseResources` | F-21 |

### 1.6 Dữ liệu lưu → ca

| Dữ liệu | Nơi lưu | Mặc định | Ca |
|---|---|---|---|
| Style, Volume (7 kênh), Rung | SharedPreferences `drum_settings` | Classic, 80, tắt | D-01, D-03 |
| Điểm cao nhất Lesson | SharedPreferences `drum_lessons`, key `best_<n>` | không có | D-01, C-10, F-41 |
| 12 âm trống | chép từ `assets/drum/` sang `files/drum_sounds/` (qua file `.part`) | — | D-02 |
| Bản thu | Room `my_record` + `files/records/*.wav` | — | F-61, D-04 |

### 1.7 Đầu vào ngoài → ca

| Đầu vào | Ca |
|---|---|
| File nhạc MediaStore (có thể bị xoá, hỏng, rất lớn, tên dài, < 10 s) | C-06, C-07, P-03, U-05, F-51 |
| Quyền READ_MEDIA_AUDIO (≥ 33) / READ_EXTERNAL_STORAGE (≤ 32) | Q-01..Q-05 |
| Quyền WRITE_EXTERNAL_STORAGE khi Download (≤ 28) | Q-06 |
| File MIDI trong assets (37 bài: 1–35, 37, 38; **không có 36**) | A-03, F-38 |
| Extra `drum_lesson_number` (từ màn kết quả) | F-35, F-36, L-07 |

### 1.8 Ngưỡng / công thức → ca

| Ngưỡng (code) | Giá trị | Ca |
|---|---|---|
| Practice Perfect / Good (`DrumLessonPractice`) | < 1000 ms Perfect · < 2000 ms Good · ≥ 2000 ms không cộng điểm (vẫn sang nốt) | A-04, F-36 |
| Miss | sai mặt: +1 **một lần mỗi nốt**, lần sai tiếp theo của cùng nốt chỉ rung | A-04, F-34 |
| Điểm | `min((P + 0.5·G − M) / tổng × 100, 100)`, cắt phần thập phân, kẹp ≥ 0 | A-05, F-37 |
| Sao **danh sách** | `DrumLessonPractice.stars`: ≥ 90 → 3 · ≥ 50 → 2 · ≥ 20 → 1 | F-40 |
| Sao **màn kết quả** | `LessonScoring.stars` (của Pad): < 26 → 0 · < 50 → 1 · < 90 → 2 · còn lại 3 | F-40 (**nghi lệch**, xem R-08) |
| Nhãn kết quả | ≥ 90 "Lesson complete!" · ≥ 26 "Nice effort!" · còn lại "Try again" | F-37 |
| Next | điểm ≥ 30 **và** chưa phải bài cuối (38) | F-36, F-38 |
| Đếm ngược / khoảng nghỉ trước Practice | 3 × 1000 ms / 600 ms | F-31 |
| Âm lượng | 0..100, mặc định 80; Snare / Tom / Splash / Ride luôn 100% | F-22, F-23 |
| Rung | 80 ms | F-21 |
| Bài nhạc tối thiểu | ≥ 10 s (bài ngắn hơn không hiện) | F-51 |
| REC tối đa | 15 phút thì tự Stop | F-69 |
| Tên bản thu | trim, gộp khoảng trắng, tối đa 60 ký tự, rỗng → báo lỗi; mặc định `Record_<epoch giây>` | F-63 |
| Hộp thoại REC màn ngang | thẻ ≤ 343dp | U-07 |

### 1.9 Luồng song song → ca

| Kết hợp | Ca |
|---|---|
| REC + mở màn khác (bị khoá) | S-01 |
| REC + Back / về nền | S-02, L-03 |
| REC + Lesson demo (tiếng demo có vào bản thu) | S-03 |
| REC + nhạc nền (pause / play / hết bài / đổi bài giữa lúc thu) | F-62, S-09 |
| Nhạc nền + mở Lesson / Style / Volume (nhạc phải tạm dừng) | S-04 |
| Lesson + âm chưa nạp xong | S-05 |
| Lesson + đổi màn / chọn bài khác / Back | S-06, S-07, S-08 |
| Nghe thử ở Choose music + về nền | S-10 |

### 1.10 Rủi ro đọc từ code → ca

| # | Rủi ro | Vị trí | Ca |
|---|---|---|---|
| R-01 | `MediaRecordPlayer.play()` gọi `prepare()` đồng bộ trên main thread: file lớn / thẻ nhớ chậm có thể đứng UI hoặc ANR | `data/audio/MediaRecordPlayer.kt` | P-03, C-07 |
| R-02 | `DrumKitView` vẽ lặp mãi khi `target` còn khác null (Practice). Nếu target không được xoá thì tốn CPU / pin | `DrumKitView.onDraw`, `DrumActivity.renderLesson` | P-02, L-04 |
| R-03 | Practice không nhận chạm khi âm chưa nạp: `play()` trả false **trước** `lesson.onHit`, có thể kẹt bài | `DrumViewModel.play` | S-05 |
| R-04 | Bài đã chọn bị xoá / mất quyền: `resume()` của player cũ vs `play()` mới | `DrumViewModel.toggleSong` | C-06, L-08 |
| R-05 | Replay khi DrumActivity đã bị huỷ: mở Drum mới bằng extra; `savedInstanceState != null` thì bỏ qua extra | `DrumActivity.initConfig` | L-07 |
| R-06 | Bấm dồn Replay / Next: nhiều `startLesson` → nhiều `onNewIntent` | `DrumLessonResultActivity` | C-04 |
| R-07 | `DrumLessonRepositoryImpl` đọc `prefs.all` trong constructor (IO trên main lần đầu) | `data/drumlesson/` | P-04 |
| R-08 | **Sao danh sách dùng mốc 20 / 50 / 90 nhưng sao màn kết quả dùng `LessonScoring.stars` (26 / 50 / 90 của Pad)** → điểm 20–25: danh sách 1 sao, kết quả 0 sao | `DrumLessonResultActivity.kt:53` vs `DrumLessonListActivity.kt:129` | F-40 |
| R-09 | Đổi ngôn ngữ khi Drum nằm trong back stack: activity tạo lại → state VM mất (bài nhạc đã chọn) | `DrumActivity` | L-12 |
| R-10 | Hộp thoại REC trên màn ngang làm hiện lại thanh hệ thống (đã sửa bằng `matchHostSystemBars`, **chưa xác nhận trên máy**) | `RecordDialogs` | U-07 |
| R-11 | Chạm dồn rất nhanh: SoundPool giới hạn số stream (âm cũ bị cắt) | `SoundPoolPadPlayer` | C-01, H-05 |
| R-12 | Thu hồi quyền khi app đang chạy → Android kill process; khi mở lại phải không crash | — | L-08 |

### 1.11 Khác biệt đã chốt so với app mẫu → ca

| Quyết định (PROGRESS) | Ca |
|---|---|
| Bố cục / flow theo app mẫu, hình theo Figma T04 | U-01, U-02 |
| Bỏ dòng Language ở Volume (đã có ở Settings) | F-20 |
| Style chỉ đổi ảnh, giữ vị trí + âm | F-11 |
| REC mix sample ra file (không thu micro), có nhạc nền theo đoạn | F-62 |
| Điểm Practice: kẹp ≥ 0 (app mẫu có thể âm) | A-05 |
| Chưa khoá Style / Lesson bằng quảng cáo | F-12, F-30 |

---

## 2. Checklist chung theo màn (K1–K15 của form)

Màn: **Drum** · **Style** · **Volume** · **Choose music** · **Lesson list** · **Lesson result** · **Hộp thoại REC**
(Save / Saved / Progress, kiểm trên Drum).

| Mục | Drum | Style | Volume | Choose music | Lesson list | Lesson result | Hộp thoại REC |
|---|---|---|---|---|---|---|---|
| K1 Mở không crash | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ |
| K2 Back đúng | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ |
| K3 Nút quay về trên màn | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ |
| K4 Back × 2 nhanh | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ |
| K5 Home → quay lại | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ |
| K6 Khoá màn → mở | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ |
| K7 Kill ở nền → mở lại | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ |
| K8 Bấm dồn nút | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ |
| K9 Loading / rỗng / lỗi | ⏭ | — | — | ⏭ | — | — | ⏭ |
| K10 UI en | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ |
| K11 UI vi | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ |
| K12 Tai thỏ / thanh hệ thống | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ |
| K13 contentDescription, ≥ 48dp | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ |
| K14 Leak 5 vòng | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | — |
| K15 check_crash sạch | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ | ⏭ |

---

## 3. Ca test theo nhóm

### A. Tự động

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| A-01 | P0 | Toàn bộ unit test | `./gradlew :app:testDebugUnitTest` | Pass hết | Unit | ⏭ |
| A-02 | P1 | Lint | `./gradlew :app:lintDebug` | Không có Error mới trong `feature/drum`, `feature/record` | Unit | ⏭ |
| A-03 | P0 | Đọc đủ 37 file MIDI | `MidiDrumParserTest` | Mọi bài có nốt, map đúng mặt (48 → Tom phải, 47 → Tom giữa, 45 → Tom trái) | Unit | ⏭ |
| A-04 | P0 | Ngưỡng Practice | `DrumLessonPracticeTest`: 999 / 1000 / 1999 / 2000 ms; sai 2 lần cùng nốt | Perfect / Good / Good / không cộng; Miss chỉ +1 | Unit (bổ sung test nếu thiếu mốc) | ⏭ |
| A-05 | P1 | Công thức điểm | P = 0, M = tổng → 0 (không âm); P = tổng → 100; P = 3, G = 1, M = 1, tổng 4 → 62 | Đúng giá trị | Unit | ⏭ |
| A-06 | P1 | Controller: chạm khi chưa Practice, cancel giữa demo | `DrumLessonControllerTest` | Không tính điểm; sau cancel không còn DemoHit | Unit | ⏭ |
| A-07 | P1 | Bài không tồn tại (36, 999) | Controller `start(36)` với repo thật / giả | `LoadFailed`, state về null | Unit | ⏭ |
| A-08 | P1 | Settings: gain từng mặt, mặc định 80, key lạ | `DrumSettingsTest` | Đúng | Unit | ⏭ |
| A-09 | P1 | REC: chạm trống `chokeGroup = null` + đoạn nhạc nền | `RecordingSessionTest`, `BeatMixerTest` | Không cắt âm chồng; đoạn nhạc đúng vị trí | Unit | ⏭ |

### F. Chức năng

**Bộ trống**

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| F-01 | P0 | Mở Drum | Home → thẻ DRUM | Màn ngang, ẩn thanh hệ thống, đủ 13 mặt; mờ + "đang chuẩn bị" rồi hết mờ (< 2 s) | ADB | ⏭ |
| F-02 | P0 | Chạm từng mặt | Chạm lần lượt 13 mặt (toạ độ ở mục 5) | Mỗi mặt nảy; logcat / nghe đúng âm; hai Kick chung một âm | ADB + Tay (nghe) | ⏭ |
| F-03 | P1 | Mặt chồng nhau | Chạm vùng giao Snare / Kick, Ride / Close HH, Crash trái / Floor | Mặt vẽ sau (nằm trên) nhận chạm | ADB | ⏭ |
| F-04 | P1 | Chạm ngoài mặt / mép vùng chạm | Chạm nền gỗ; chạm ở 0.85 và 0.95 bán kính một mặt | Nền: không có tiếng; 0.85 trúng, 0.95 trượt (vùng chạm 0.9) | ADB | ⏭ |
| F-05 | P1 | Multi-touch | Hai ngón cùng lúc Kick + Snare; ba ngón | Cả hai / ba cùng phát, cymbal ngân chồng không cắt nhau | Tay | ⏭ |
| F-06 | P1 | Ẩn / hiện thanh | Nút Ẩn → nút mắt góc phải → bấm | Thanh ẩn, bộ trống không đổi vị trí; bấm mắt thì hiện lại | ADB | ⏭ |
| F-07 | P1 | Chạm khi âm chưa sẵn sàng | Ngay khi mở Drum (đang mờ) chạm dồn | Toast "chưa nạp âm" **chỉ một cái** (toast dùng lại), không xếp hàng | ADB | ⏭ |
| F-08 | P2 | Metronome | Bấm Metronome | Toast "Coming soon" | ADB | ⏭ |
| F-09 | P0 | Thoát | Bấm Home trên thanh; vào lại rồi bấm Back hệ thống | Về màn chính, âm dừng | ADB | ⏭ |

**Style**

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| F-10 | P0 | Đổi Style | Style → Minimal → Apply | Về Drum, ảnh đổi, toast "Style has been applied." | ADB | ⏭ |
| F-11 | P1 | Đủ 4 Style, vị trí / âm giữ nguyên | Lần lượt Classic, Minimal, Bass House, EDM; mỗi lần chạm 3 mặt | Ảnh đúng bộ; vùng chạm và âm không đổi; snare Classic không lệch | ADB + Tay | ⏭ |
| F-12 | P1 | Nhãn Applied, chọn lại style đang dùng | Style: thẻ đang dùng có "Applied"; chọn nó → Apply | Về Drum, **không** có toast | ADB | ⏭ |
| F-13 | P1 | Back không lưu | Style → EDM → Back | Drum giữ style cũ | ADB | ⏭ |

**Volume**

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| F-20 | P1 | Nội dung màn | Mở Volume | Rung (tắt), 7 thanh đúng tên + 80, Reset everything; không có dòng Language | ADB | ⏭ |
| F-21 | P2 | Rung | Bật Rung → về Drum → chạm | Rung khoảng 80 ms mỗi chạm; tắt thì không rung | Tay | ⏭ |
| F-22 | P0 | Kênh trái / phải độc lập | Kick trái = 0, Crash phải = 0 → về Drum | Kick trái, Crash phải im; Kick phải, Crash trái vẫn kêu | ADB + Tay | ⏭ |
| F-23 | P2 | Mặt không có thanh | Kéo hết 7 thanh về 0 | Snare, Tom, Splash, Ride vẫn kêu 100% | Tay | ⏭ |
| F-24 | P1 | Lưu ngay khi kéo | Kéo thanh → Back (không Apply) → mở lại Volume | Giá trị vẫn giữ | ADB | ⏭ |
| F-25 | P1 | Reset | Reset everything | 7 thanh về 80, Rung tắt | ADB | ⏭ |

**Lesson**

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| F-30 | P0 | Danh sách | Lesson | 2 cột, 37 bài ("Lesson 1"… không có 36), sao / % theo điểm cao nhất, Learn (chưa chơi) / Replay | ADB | ⏭ |
| F-31 | P0 | Luồng đủ | Chọn Lesson 1 | Nút Lesson ghi "Lesson 1"; phủ đếm 3-2-1 (mỗi bước 1 s) → nhãn "Nghe demo", demo tự đánh có tiếng + mặt nảy → sau 600 ms nhãn "Đến lượt bạn 0 / y", mặt cần đánh nhấp nháy | ADB + chụp | ⏭ |
| F-32 | P1 | Demo đúng nhịp | Ghi logcat / quay màn lúc demo | Khoảng cách các nốt đúng file MIDI (không trôi) | ADB | ⏭ |
| F-33 | P0 | Đánh đúng hết, nhanh | Chạm đúng mặt nhấp nháy trong < 1 s mỗi nốt (TAPS theo nốt) | Kết quả 100%, 3 sao, Perfect = tổng, Miss 0 | ADB | ⏭ |
| F-34 | P0 | Đánh sai | Chạm sai mặt 2 lần ở cùng nốt rồi đánh đúng | Mặt sai rung mỗi lần; Miss chỉ +1; bài vẫn đi tiếp | ADB | ⏭ |
| F-35 | P1 | Chạm khi đang đếm / demo | Chạm liên tục trong lúc đếm và demo | Có tiếng, không tính điểm, demo không lệch nhịp, Practice bắt đầu từ 0 | ADB | ⏭ |
| F-36 | P1 | Good / chậm | Mỗi nốt chờ 1.5 s rồi đánh; lượt khác chờ 2.5 s | 1.5 s → toàn Good (50%), có Next; 2.5 s → 0%, không Next | ADB | ⏭ |
| F-37 | P1 | Nhãn kết quả theo điểm | Tạo 3 lượt: ≥ 90, 26–89, < 26 | "Lesson complete!" / "Nice effort!" / "Try again" | ADB | ⏭ |
| F-38 | P1 | Next / bài cuối | Hoàn thành bài 35 với điểm ≥ 30 → Next; hoàn thành bài 38 | 35 → Next mở bài **37** (bỏ qua 36); 38 không có Next; "Lesson x of 37" đúng | ADB | ⏭ |
| F-39 | P0 | Replay / Home / Back ở kết quả | Replay; lượt khác Home; lượt khác Back | Replay: về **đúng** Drum cũ (không chồng màn), chạy lại từ đếm; Home: về màn chính; Back: về Drum chơi tự do | ADB | ⏭ |
| F-40 | P1 | Sao danh sách = sao kết quả | Đạt điểm trong khoảng 20–25 (vd. tổng 8: P1, G1, 0 Miss → 18; tìm tổ hợp ra 20–25) → so sao ở kết quả và danh sách | **Hai nơi cùng số sao.** Nghi lỗi R-08: kết quả 0 sao, danh sách 1 sao | ADB | ⏭ |
| F-41 | P1 | Giữ điểm cao nhất | Chơi bài đã có 100% với điểm thấp | Danh sách vẫn 100%; màn kết quả hiện điểm lượt này | ADB | ⏭ |

**Add Song**

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| F-50 | P0 | Mở Choose music (đã có quyền) | Add Song | Mở Choose music; nếu nhạc nền đang phát thì đã tạm dừng | ADB | ⏭ |
| F-51 | P1 | Danh sách và nghe thử | Xem danh sách; nghe thử A rồi B; bấm lại B | Không có bài < 10 s; chỉ một bài phát; bấm lại thì dừng | ADB + Tay | ⏭ |
| F-52 | P0 | Chọn bài | Đang nghe thử A → bấm + của B | Nghe thử dừng; về Drum; nút Play + tên B chạy chữ; **chưa tự phát** | ADB | ⏭ |
| F-53 | P0 | Phát / tạm dừng / tiếp | Play → 5 s → Pause → Play | Phát tiếp đúng chỗ dừng; icon đổi đúng | ADB + Tay | ⏭ |
| F-54 | P1 | Hết bài | Để bài phát hết | Nút về Play; bấm Play thì phát lại từ đầu | ADB | ⏭ |
| F-55 | P1 | Trống + nhạc cùng lúc | Đang phát nhạc, chạm trống dồn | Cả hai cùng nghe, nhạc không bị ngắt | Tay | ⏭ |
| F-56 | P1 | Đổi bài khi bài cũ đang phát | Phát A → Add Song → chọn B | A dừng; hiện B, chưa phát | ADB | ⏭ |

**REC trên Drum**

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| F-60 | P0 | Thu cơ bản | REC → chạm 8 mặt trong 5 s → Stop | Nút đỏ + mm:ss chạy; Stop → hộp Save record, tên mặc định `Record_<epoch>` | ADB | ⏭ |
| F-61 | P0 | Lưu và phát lại | Save → My records → phát | Có bản thu mới, thời lượng ≈ 5 s, nghe đúng nhịp, đúng âm lượng từng mặt (F-22) | ADB + Tay | ⏭ |
| F-62 | P0 | Thu có nhạc nền | Play bài → REC → chạm → Pause 3 s → chạm → Play → Stop → Save | File có nhạc ở đoạn đang phát, đoạn pause chỉ có trống; nhạc tiếp đúng chỗ | Tay | ⏭ |
| F-63 | P1 | Tên bản thu | Tên rỗng / chỉ khoảng trắng; 80 ký tự; tiếng Việt có `/ :` | Rỗng → báo lỗi, không lưu; dài → cắt 60; ký tự đặc biệt lưu được | ADB (TYPE) | ⏭ |
| F-64 | P1 | Cancel | REC → Stop → Cancel / X | Không có bản thu mới; bấm REC lại được | ADB | ⏭ |
| F-65 | P1 | Download từ hộp Saved | Saved → Download | Tiến trình → toast thành công; file trong `Music/Beat Maker` | ADB | ⏭ |
| F-66 | P1 | Share | Saved → Share | Mở share sheet với file WAV | ADB | ⏭ |
| F-67 | P1 | Mở My records | Saved → My records | Mở My records; Back về Drum, REC ở trạng thái thường | ADB | ⏭ |
| F-68 | P2 | REC rồi Stop ngay | REC → Stop sau < 1 s, không chạm | Hành vi hợp lệ (hỏi lưu bản trống hoặc bỏ qua), không crash | ADB | ⏭ |
| F-69 | P2 | Tự Stop ở 15 phút | Thu liên tục 15 phút | Tự Stop + hỏi lưu; file lưu được | Tay (dài) | ⏭ |

### S. Trạng thái kết hợp

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| S-01 | P0 | Khoá nút khi đang thu | REC → bấm Lesson, Style, Add Song, Metronome, Volume | Mờ, không mở gì; Home trên thanh = Stop | ADB | ⏭ |
| S-02 | P0 | Back khi đang thu | REC → Back | Stop + hộp Save, **không** rời màn | ADB | ⏭ |
| S-03 | P1 | Thu trong lúc Lesson demo | Chọn bài → REC trong lúc đếm (nút REC vẫn bấm được?) → demo → Stop → Save | Bản thu có tiếng demo; Lesson không bị huỷ vì REC | ADB + Tay | ⏭ |
| S-04 | P1 | Nhạc nền + mở màn khác | Phát nhạc → Lesson / Style / Volume | Nhạc tạm dừng, quay lại vẫn ở Pause (không tự phát) | ADB | ⏭ |
| S-05 | P1 | Lesson khi âm chưa sẵn sàng | Ở kết quả bấm Replay ngay sau khi kill (Drum tạo mới, âm đang nạp); hoặc làm nạp lỗi | Không kẹt Practice: chạm được sau khi nạp xong, hoặc có thông báo | ADB | ⏭ |
| S-06 | P1 | Mở màn khác giữa Lesson | Đang Practice → Style → Back | Lesson đã huỷ, không còn mặt nhấp nháy, nút ghi "Lesson" | ADB | ⏭ |
| S-07 | P1 | Chọn bài khác giữa Lesson | Bài 1 đang demo → Lesson → bài 2 | Bài 1 dừng hẳn (không còn tiếng demo bài 1), bài 2 chạy từ đếm | ADB | ⏭ |
| S-08 | P0 | Back giữa Lesson | Đang demo → Back; đang Practice → Back | Dừng bài, ở lại Drum; Back thêm lần nữa thì thoát | ADB | ⏭ |
| S-09 | P2 | Đổi bài / hết bài khi đang thu | Thu → bài hết → phát lại; thu → Add Song bị khoá | Đoạn nhạc nền trong file đúng như đã nghe | Tay | ⏭ |
| S-10 | P1 | Nghe thử rồi về nền | Choose music đang nghe thử → Home | Nghe thử dừng | ADB | ⏭ |

### L. Vòng đời / gián đoạn

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| L-01 | P0 | Home → quay lại | Đang chơi → Home → Recent | Không còn tiếng khi ở nền; quay lại chơi được ngay, không mờ | ADB | ⏭ |
| L-02 | P0 | Khoá màn khi phát nhạc | Phát nhạc → khoá → mở | Nhạc tạm dừng khi khoá, nút Play; mở lại thì thanh hệ thống vẫn ẩn | ADB | ⏭ |
| L-03 | P0 | Về nền khi đang thu | REC → Home → quay lại | Đã Stop, hộp Save đang hiện; Save được | ADB | ⏭ |
| L-04 | P0 | Về nền giữa Lesson | Practice → Home → quay lại | Lesson huỷ, không mặt nhấp nháy, không còn vẽ lặp (P-02) | ADB | ⏭ |
| L-05 | P0 | Kill ở Drum | Home → `am kill` → mở từ Recent | Drum mở lại, nạp âm, chơi được, style / volume giữ; bài nhạc đã chọn mất (chấp nhận) | ADB | ⏭ |
| L-06 | P1 | Kill ở Style / Volume / Choose music / Lesson list | Mỗi màn: Home → kill → mở lại → Back | Không crash, về Drum | ADB | ⏭ |
| L-07 | P1 | Kill ở màn kết quả rồi Replay | Kết quả → Home → kill → mở lại → Replay | Mở Drum và chạy đúng bài (R-05) | ADB | ⏭ |
| L-08 | P1 | Thu hồi quyền nhạc khi đã chọn bài | Chọn bài → Home → `pm revoke` → mở lại → Play | Process bị kill → mở lại không crash; Play báo lỗi / không có bài | ADB | ⏭ |
| L-09 | P1 | Kill khi đang mở hộp Save record | REC → Stop → Home → kill → mở lại | Không crash (lượt thu tạm mất là chấp nhận) | ADB | ⏭ |
| L-10 | P2 | Xoay 180° | Xoay ngược máy | Màn xoay, trạng thái giữ, tai thỏ chừa đúng phía mới | Tay | ⏭ |
| L-11 | P2 | Hiện thanh hệ thống bằng vuốt mép | Vuốt từ mép trên | Thanh hiện tạm rồi tự ẩn; không đẩy layout | Tay | ⏭ |
| L-12 | P1 | Đổi ngôn ngữ khi Drum trong back stack | Drum → (Home của app) → Settings → đổi Language → quay lại Drum nếu còn | Không crash, chữ đổi ngôn ngữ (R-09) | ADB | ⏭ |
| L-13 | P2 | Chia đôi màn hình (multi-window) | Mở Drum trong split screen (nếu ROM cho) | Không crash; bộ trống co theo khung | Tay | ⏭ |

### C. Crash / biên

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| C-01 | P0 | Chạm dồn | `TAPS` 200 chạm, 10 ms một lần, trên 13 mặt | Không crash / ANR; vẫn có tiếng sau đó | ADB | ⏭ |
| C-02 | P0 | Bấm dồn nút mở màn | Style / Volume / Lesson / Add Song mỗi nút × 10 trong 300 ms | Mỗi nút chỉ mở **một** màn | ADB | ⏭ |
| C-03 | P1 | Bấm dồn REC | REC × 11 nhanh | Không crash; cuối cùng: đang thu hoặc hộp Save, chỉ một hộp | ADB | ⏭ |
| C-04 | P1 | Bấm dồn Replay / Next | Kết quả: Replay × 5 nhanh | Một lượt Lesson; một màn Drum | ADB | ⏭ |
| C-05 | P2 | Bấm dồn Save trong hộp tên | Save × 5 nhanh | Chỉ một bản thu | ADB | ⏭ |
| C-06 | P1 | File nhạc bị xoá sau khi chọn | Chọn bài (bản copy trong Download) → xoá file → Play | Toast lỗi, bỏ bài, không crash | ADB | ⏭ |
| C-07 | P1 | File nhạc hỏng | Chọn `.mp3` rác (nếu MediaStore liệt kê) → Play | Toast lỗi, không crash / ANR | ADB | ⏭ |
| C-08 | P2 | Không có bài nào | (emulator hoặc thu hồi quyền media rồi cấp lại khi không có nhạc) | Thông báo "Không tìm thấy bài nhạc nào" | ⏭ emulator | ⏭ |
| C-09 | P0 | Mở / đóng Drum liên tục | Home ↔ Drum 15 lần nhanh (không chờ nạp xong) | Không crash; lần cuối chơi được | ADB | ⏭ |
| C-10 | P2 | Dữ liệu điểm hỏng | `run-as` sửa `best_5` thành chuỗi, `best_7` = 999 trong `drum_lessons.xml` | Không crash; bài 5 coi như chưa chơi, bài 7 hiện 100% | ADB | ⏭ |
| C-11 | P2 | Nạp âm lỗi → Thử lại | Xoá `files/drum_sounds/` khi app ở nền và chặn ghi (khó tạo); tối thiểu: kiểm hộp lỗi qua code review | Hộp lỗi + Thử lại hoạt động | ⏭ / review | ⏭ |
| C-12 | P1 | Chạm khi màn kết quả đang mở | Hoàn thành bài, chạm dồn ngay lúc chuyển màn | Không mở 2 màn kết quả | ADB | ⏭ |

### M. Leak / tài nguyên

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| M-01 | P0 | Leak Drum | Ở Home: `leak_check.sh truoc` → Drum (chạm vài mặt, REC → Cancel) → Back × 10 vòng → `leak_check.sh sau` | Activities = 1 (Main); Views không tăng theo số vòng; PSS +< 5 MB | ADB | ⏭ |
| M-02 | P1 | Leak màn con | Từ Drum: Style, Volume, Choose music, Lesson list mỗi màn mở / đóng 5 lần → đo | Activities = 2 (Main + Drum) | ADB | ⏭ |
| M-03 | P0 | Không còn âm ngầm | Phát nhạc + chạm trống → Back về Home → `dumpsys audio` | Không còn player `started` của app | ADB | ⏭ |
| M-04 | P1 | Sau nhiều Lesson | 3 bài liên tiếp qua Replay / Next → Home → đo | Activities = 1 | ADB | ⏭ |
| M-05 | P2 | File tạm | Sau F-60..F-68: `run-as ls files/records files/drum_sounds cache` | Không còn file `.part` / file thu thừa sau Cancel | ADB | ⏭ |

### U. Giao diện

Mỗi ca: chụp màn (`build/test-evidence/drum/<lượt>/U-xx_<ngôn ngữ>.png`) + `ui_dump.sh`, làm với **en** và **vi**.

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| U-01 | P0 | Drum chơi tự do | Chụp | Thanh trên đủ 8 nút, nhãn không cắt ("Kiểu trống", "Thêm nhạc", "Âm lượng"); bộ trống không bị tai thỏ che, không tràn | ADB | ⏭ |
| U-02 | P1 | Thanh ẩn | Ẩn thanh → chụp | Nút mắt không đè lên mặt trống, ≥ 48dp | ADB | ⏭ |
| U-03 | P1 | Đang nạp / lỗi nạp | Chụp ngay khi mở | Hộp trạng thái giữa màn, chữ không cắt | ADB | ⏭ |
| U-04 | P0 | Lesson | Chụp lúc Countdown, Demo, Practice | Lớp đếm phủ đủ; nhãn "Bài học 1 · Đến lượt bạn 12 / 40" không cắt, không đè thanh / trống; nút Lesson "Lesson 12" vừa khung | ADB | ⏭ |
| U-05 | P1 | Tên bài dài | Chọn bài tên dài | Tên chạy chữ trong khung nút, các nút khác không bị đẩy | ADB | ⏭ |
| U-06 | P1 | Đang thu | Chụp khi REC 1 phút | Nút đỏ "01:00" không làm lệch thanh; 5 nút mờ rõ ràng | ADB | ⏭ |
| U-07 | P0 | Hộp thoại REC màn ngang | Save record (bàn phím mở), Saved, Progress | Thẻ ≤ 343dp, giữa màn; bàn phím không che Save; **thanh hệ thống vẫn ẩn** (R-10) | ADB | ⏭ |
| U-08 | P1 | Style | Chụp en / vi | Thẻ 2 cột không cắt; nhãn "Đang dùng"; nút Áp dụng đủ | ADB | ⏭ |
| U-09 | P1 | Volume | Chụp en / vi, cuộn xuống cuối | 7 nhãn không cắt; cuộn tới "Đặt lại tất cả"; thanh trượt đủ dài | ADB | ⏭ |
| U-10 | P1 | Choose music | Chụp có bài / rỗng / đang nghe thử | Tên dài có "…"; icon play / + không bị đẩy ra ngoài | ADB | ⏭ |
| U-11 | P1 | Lesson list | Chụp đầu / cuối danh sách | Thẻ đều, "Lesson 38" không cắt, sao / % thẳng hàng | ADB | ⏭ |
| U-12 | P1 | Lesson result | Chụp 0, 1, 2, 3 sao; có / không Next | Không cắt chữ ("Lần thử lại" / "Nice effort!"); nút đủ, không chồng | ADB | ⏭ |

### P. Hiệu năng

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| P-01 | P1 | Khung hình khi chơi | `gfxinfo reset` → chạm dồn 15 s → `gfxinfo` | Janky ≤ 10%, p90 ≤ 16 ms | ADB | ⏭ |
| P-02 | P1 | Không vẽ khi rảnh | Chơi tự do, không chạm 5 s; sau L-04 cũng đo | Frames ≈ 0 (không lặp `postInvalidateOnAnimation`) | ADB | ⏭ |
| P-03 | P1 | Phát bài lớn | Play bài lớn nhất (> 8 MB) | Không ANR; UI đứng < 200 ms (đo `Choreographer: Skipped frames`) | ADB | ⏭ |
| P-04 | P2 | Thời gian mở màn | Logcat `Displayed ...DrumActivity`, `...DrumLessonListActivity` | Ghi số liệu; Drum < 800 ms | ADB | ⏭ |
| P-05 | P2 | Độ trễ chạm → tiếng | Cảm nhận / quay video 240fps | Không thấy trễ rõ rệt (< 100 ms) | Tay | ⏭ |
| P-06 | P2 | CPU khi rảnh | `top -n 1 -p <pid>` sau 10 s không chạm | CPU ≈ 0–2% | ADB | ⏭ |

### D. Dữ liệu

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| D-01 | P0 | Còn sau force-stop | Đổi Style, 1 thanh Volume, Rung, chơi 1 bài → `am force-stop` → mở lại | Còn đủ | ADB | ⏭ |
| D-02 | P1 | Âm trống tự chép lại | `run-as rm files/drum_sounds/snare.ogg` (app ở nền, đã kill) → mở Drum → chạm Snare | Có tiếng; file được chép lại | ADB | ⏭ |
| D-03 | P2 | Giá trị prefs lạ | Sửa style thành `"abc"`, volume = 150 / -5 | Style về Classic; volume bị kẹp 0..100, không crash | ADB | ⏭ |
| D-04 | P1 | Bản thu còn sau kill | Lưu bản thu → kill → My records | Còn, phát được | ADB | ⏭ |

### Q. Quyền

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| Q-01 | P0 | Xin quyền lần đầu → cho phép | `pm revoke` quyền nhạc → Add Song → Cho phép | Mở Choose music ngay | ADB | ⏭ |
| Q-02 | P1 | Từ chối một lần | Add Song → Từ chối | Toast "Cho phép truy cập nhạc…", ở lại Drum | ADB | ⏭ |
| Q-03 | P1 | Từ chối hẳn | Từ chối 2 lần → Add Song | Toast + mở Cài đặt app; quay lại không crash | ADB | ⏭ |
| Q-04 | P1 | Cấp trong Cài đặt rồi quay lại | Từ Q-03 bật quyền → Back | Add Song mở được | Tay | ⏭ |
| Q-05 | P2 | API 33+ (READ_MEDIA_AUDIO) | Như Q-01 trên emulator 33+ | Như Q-01 | emulator | ⏭ |
| Q-06 | P2 | Download trên Android ≤ 9 | Saved → Download → từ chối / cho phép | Từ chối: báo; cho phép: lưu được (kể cả khi activity bị tạo lại) | emulator 28 | ⏭ |

### H. Âm thanh / phần cứng

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| H-01 | P2 | App khác phát nhạc | Mở app nhạc khác phát → quay lại Drum → phát bài | Ghi hiện trạng (audio focus chưa làm - Phase 5) | Tay | ⏭ |
| H-02 | P2 | Cuộc gọi đến khi đang phát / đang thu | Gọi vào máy | Ghi hiện trạng; tối thiểu không crash, lượt thu không mất | Tay | ⏭ |
| H-03 | P2 | Rút tai nghe khi phát nhạc | Rút tai nghe | Ghi hiện trạng (nên tạm dừng) | Tay | ⏭ |
| H-04 | P2 | Nút âm lượng | Bấm vol +/- trong Drum | Chỉnh luồng media (không phải chuông) | Tay | ⏭ |
| H-05 | P2 | Nhiều âm chồng | Chạm 20 mặt cymbal trong 1 s | Không méo / rè nặng; giới hạn stream cắt âm cũ hợp lý | Tay | ⏭ |

### X. Trợ năng

| ID | Ưu tiên | Ca test | Bước | Kỳ vọng | Chạy | KQ |
|---|---|---|---|---|---|---|
| X-01 | P2 | contentDescription | `ui_dump.sh` ở Drum, Style, Volume, Choose music | Các nút icon (mắt, play / pause, nghe thử, +) có mô tả | ADB | ⏭ |
| X-02 | P2 | Vùng chạm | bounds các nút thanh, nút mắt, nút + | ≥ 48dp (≥ 96 px ở 320 dpi) | ADB | ⏭ |

### Nhóm không áp dụng

- **N (Mạng):** Drum không dùng mạng (âm trống, MIDI đều nằm trong assets).
- **G (Quảng cáo):** Drum chưa gắn quảng cáo (PROGRESS mục 1).
- **V (Nâng cấp):** chưa có bản phát hành trước; prefs `drum_settings` / `drum_lessons` mới. Kiểm khi có bản 2.
- **T (Tương thích):** thể hiện ở bảng máy (mục 0); emulator API 24 / 33 chạy khi có AVD.

---

## 4. Bộ smoke (P0)

A-01, A-03, A-04, F-01, F-02, F-09, F-10, F-22, F-30, F-31, F-33, F-34, F-39, F-50, F-52, F-53, F-60, F-61, F-62,
S-01, S-02, S-08, L-01..L-05, C-01, C-02, C-09, M-01, M-03, U-01, U-04, U-07, D-01, Q-01.

---

## 5. Phụ lục: toạ độ chạm (máy RMX3231, màn ngang 1600×720)

Điền sau khi đo `drumKit` bằng `ui_dump.sh`. Công thức: `s = min(w/751.02, h/409)`, `offX = left + (w − 751.02·s)/2`,
`offY = top + (h − 409·s)/2`, `x = offX + cx·s`, `y = offY + cy·s` (`cx`, `cy` lấy trong `DrumKitView.pieces`).

| Mặt | cx, cy (design) | x, y (px) |
|---|---|---|
| (chưa đo) | | |

---

## 6. Báo cáo các lượt

(chưa chạy)
