# QR Code Scanner 專案指南

## 專案定位

這是一個 Android QR Code 掃描器。專案早期曾經服務特定簡訊流程，但目前產品定位是通用掃描器：可辨識文字、網址、簡訊、電話、Email、Wi-Fi 等 QR Code 內容，並依使用者在設定頁的選項執行後續動作。

## 技術基礎

- Android application module：`app`
- Package／namespace：`com.rsps1008.qrcode`
- `minSdk 30`、`targetSdk 37`、`compileSdk 37`
- Kotlin、AndroidX、Material Components、View Binding
- CameraX 負責相機預覽與影像分析
- Google ML Kit Barcode Scanning 負責條碼／QR Code 辨識
- Room 負責掃描歷史資料
- Google Sign-In 與 Google Drive `appDataFolder` 負責歷史／最愛備份
- Gradle Wrapper：以專案內 `gradlew.bat` 執行
- Java source／target compatibility：17；目前建議使用相容的 JDK 21 執行 Gradle

## 目錄與責任

```text
app/src/main/java/com/rsps1008/qrcode/
├── QRCodeAnalyzer.kt             # CameraX ImageAnalysis 與 ML Kit 辨識
├── SettingsPreference.kt         # 設定頁、主題切換與 Google Drive 備份／還原
├── Utils.kt                      # Room database 建立與 migration 註冊
├── backup/
│   ├── GoogleDriveService.kt     # Google Drive appDataFolder 檔案上傳／下載
│   └── ScanResultBackup.kt        # 歷史與最愛狀態的版本化 JSON
└── ui/
    ├── MainActivity.kt           # 掃描主頁、ActionBar、權限、外部 Intent
    ├── MainViewModel.kt           # 掃描去重、類型分流、設定行為、網站標題抓取
    ├── ScanResultFragment.kt      # 歷史／最愛列表、左右滑除
    ├── ScanResultViewModel.kt     # 歷史查詢、最愛篩選、刪除與切換最愛
    ├── ScanResultRecyclerViewAdapter.kt
    └── database/
        ├── AppDatabase.kt         # Room schema 版本與 Entity
        ├── ScanResult.kt           # 歷史紀錄資料模型與類型
        ├── ScanResultDao.kt        # 查詢、新增、刪除、標題／最愛更新
        └── Converters.kt           # Date 欄位轉換

app/src/main/res/
├── layout/                       # 掃描主頁與歷史卡片版面
├── menu/options_menu.xml         # 主頁右上角：我的最愛、歷史、設定
├── xml/preference_main.xml       # 設定頁選項
├── values/                       # 英文、顏色、主題與尺寸
├── values-zh-rTW/                # 繁體中文文字
└── values-night/                 # 深色主題顏色與主題

docs/
├── index.html                    # qrcode.rsps1008.ru 網站首頁
├── privacy-policy/index.html     # 上架用隱私權政策頁
├── license/index.html            # MIT／原始 Apache 授權說明頁
├── styles.css                    # 網站共用樣式
├── assets/qr-code-scanner-icon.png # 由 App launcher icon 複製的網站圖示
└── CNAME                         # GitHub Pages 自訂網域
```

## 主要功能契約

### 掃描與內容處理

- `QRCodeAnalyzer` 將 CameraX 影像交給 ML Kit；目前掃描主流程會處理辨識到的第一個 Barcode。
- 掃描啟動時會檢查後鏡頭可用的 AE FPS 範圍；若有上限為 60 FPS 的範圍，Preview 與 ImageAnalysis 會共同請求該範圍，否則保留裝置預設幀率。
- 純文字保存為 `TYPE.TEXT`，歷史頁使用 ABC 圖示。
- 網址保存為 `TYPE.REDIRECT`，可取得網頁 `<title>`；標題抓取失敗時仍保留原始網址。
- SMS、電話、Email、Wi-Fi 各自保存為獨立 `TYPE`，歷史頁使用對應圖示。
- 網址、SMS、電話、Email 的自動開啟行為由設定控制；關閉時會先顯示確認提示。
- Wi-Fi 使用 Android 系統的加入網路確認流程，不由 App 靜默連線。
- 純文字的自動複製與複製後震動由設定控制。
- 「開啟後關閉 APP」若啟用，應在相關外部動作完成必要的保存／標題處理後再關閉。

### 網站標題

- 僅對網址在背景抓取 HTML `<title>`。
- 使用連線與讀取逾時，不能讓掃描主執行緒等待網路。
- 標題是輔助顯示；原始網址永遠要保存並可點擊／複製。
- 不要因為抓不到標題而刪除或阻止歷史紀錄。

### 歷史與最愛

- `ScanResult` 保存 `id`、`timestamp`、`content`、`type`、可空的 `title`、`isFavorite`。
- 主頁右上角固定提供「我的最愛」、「歷史紀錄」、「設定」三個入口。
- App 啟動後直接進入掃描流程，不顯示免責說明或同意／離開對話框。
- 歷史卡片右側星號可切換最愛；我的最愛入口顯示最愛紀錄。
- 網址歷史卡片在星號左側顯示鉛筆，可編輯保存的網站標題；清空標題時儲存為 `null` 並改為只顯示原始網址。
- 歷史卡片支援左右滑動；滑動時先顯示移除背景與文字，完成滑動後才刪除資料。
- 系統返回鍵、設定頁、歷史頁與最愛頁都應返回掃描主頁。
- 設定頁可登入 Google，將歷史與最愛狀態保存為 `qrcode_scanner_history.json` 至 Drive `appDataFolder`。
- Google Drive 還原必須先經使用者確認，並以 Room transaction 替換本機歷史；備份包含標題、類型、時間與最愛狀態，不依賴本機自動遞增 ID。

### 設定與主題

- 設定 XML 位於 `app/src/main/res/xml/preference_main.xml`。
- 每個開關都必須同時有標題與簡短、使用者導向的摘要文字。
- 英文 `values/strings.xml` 與繁體中文 `values-zh-rTW/strings.xml` 必須同步更新。
- `MainActivity` 啟用 edge-to-edge；掃描預覽可延伸至系統列下方，但設定／歷史頁所在的 `fragment_pref` 必須套用狀態列與導覽列 Insets。
- 首次啟動預設：開啟後關閉 APP 關閉；Wi-Fi、網址、SMS／電話／Email、文字複製、複製震動開啟。
- 第一次啟動時依裝置當下的明亮／深色外觀初始化；之後由設定頁的外觀選單保存並套用明亮或深色模式。
- 設定頁、歷史頁、最愛頁與掃描主頁必須使用相同的主題狀態。
- 設定頁最下方顯示目前 `versionName`，並提供 `https://qrcode.rsps1008.ru/privacy-policy/` 隱私權政策連結。
- App 主題主色與歷史紀錄卡片圖示配色使用大海藍色系，日間與夜間模式分別維持足夠對比。
- Google Drive 功能使用 `DriveScopes.DRIVE_APPDATA`；沒有登入或未授予該 scope 時，備份／還原選項必須停用。

## 資料庫規則

- 修改 `ScanResult` 欄位或 `TYPE` 前，先確認 Room schema 與既有資料的相容性。
- 目前資料庫版本為 4；`Utils.kt` 負責註冊 1→2、2→3、3→4 migration。
- 新安裝不需要為不存在的舊資料增加修正 migration；若使用者明確要求全新安裝，可移除未必要的資料清理 migration，但不能破壞目前欄位 migration。
- 既有使用者資料庫名稱與 SharedPreferences key 可能仍帶有舊產品識別字，除非另有完整搬遷計畫，不要任意改名。
- 任何資料庫變更都要產生對應 `app/schemas/.../N.json`，並驗證 build。
- `ScanResultDao.replaceAll()` 的清空與批次插入必須維持在同一個 Room transaction 內。

## 自動學習與 AGENTS.md 維護

每次完成有實質影響的程式碼、資料庫、UI 或功能變更後，Agent 必須在回覆前執行以下自我更新檢查：

1. 搜尋本檔案描述的檔案、設定 key、資料庫版本、功能入口是否仍與原始碼一致。
2. 若架構、使用者可見功能、資料模型、預設值或驗證方式改變，直接在本次變更中同步更新 `AGENTS.md`。
3. 只寫入已由原始碼、測試、編譯或實際操作確認的內容；不把猜測、暫時方案或未採用計畫寫成規則。
4. 保留使用者手動加入的專案規則；若新內容與既有規則衝突，先依目前原始碼與使用者最新要求修正，並刪除過時描述。
5. 不記錄一般 commit 訊息、短期 bug 進度或單次錯誤，除非它形成未來維護仍適用的長期契約。
6. AGENTS.md 的更新要和造成變更的程式碼同一批檢查；若使用者要求只檢查、不修改，則只報告差異，不自行更新。

這個「自動學習」是以每次任務結束時重新比對專案現況來維護本檔案，不會自行修改專案外的記憶或產生未經確認的功能。

## 修改流程

1. 先讀取相關 Kotlin、XML、Manifest、資料庫 schema 與目前設定文字。
2. 保留既有未提交變更；只修改與需求相關的檔案。
3. UI 變更要同步更新英文與繁體中文資源；資料庫變更要同步更新 schema／migration。
4. 優先用 `apply_patch` 修改檔案。
5. 完成後至少執行：

   ```powershell
   .\gradlew.bat :app:assembleDebug
   git -c safe.directory=E:/Git/QRCodeFor1922 diff --check
   ```

6. 回報時區分「原始碼／資源檢查」、「`diff --check`」與「編譯／裝置實測」；沒有裝置實測時不可宣稱已完成實機驗證。

### 網站與公開文件

- 網站使用 `docs/` 靜態檔案，正式網域為 `https://qrcode.rsps1008.ru/`；`docs/CNAME` 由維護者自行管理。
- 首頁、隱私權政策與授權頁必須使用 App icon，並維持目前專案 GitHub、原始專案 `asadman1523/QRCodeFor1922` 與 `Copyright (C) 2021 YuJhen` 的清楚歸屬說明。
- 隱私權政策只能描述目前原始碼與 Manifest 可確認的資料處理；程式行為變更時，需同步檢查 `PrivacyPolicy.md` 與 `docs/privacy-policy/index.html`。
- `LICENSE` 適用於本專案新增／修改部分；源自原始專案的程式碼仍須保留 `LICENSE-APACHE-2.0` 與原作者資訊。第三方元件列於 `THIRD_PARTY_LICENSES.md`。

## 常見注意事項

- Android Studio 的 Gradle JVM 必須使用 Gradle 8.14.5 支援的版本；本專案目前使用 JDK 21。
- `AndroidManifest.xml` 已宣告相機、網路、Wi-Fi 狀態／變更與震動權限；新增權限前先確認是否真的需要。
- 網路抓取只能在背景執行緒／coroutine 進行，並設定合理 timeout。
- Google Drive 上傳／下載只能在背景 coroutine 執行；App 僅存取自己的 `appDataFolder`，不要求完整 Drive 檔案權限。
- Intent 開啟前要確認系統存在可處理的 Activity；無法處理時應保留內容並依設定複製或顯示備援。
- 不要把一般文字誤存成 `REDIRECT`；只有 ML Kit URL 類型或明確網址格式才使用連結類型。
- 不要為了美化歷史頁移除星號的可點擊區域、網址備援或左右滑除的移除過渡效果。
