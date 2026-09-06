# QR Code Scanner 隱私權政策

最後更新日期：2026 年 9 月 6 日

本政策適用於 QR Code Scanner Android 應用程式。網站版政策請參閱 <https://qrcode.rsps1008.ru/privacy-policy/>。

## 開發者資訊

- 維護者：rsps1008
- 原始專案：[asadman1523/QRCodeFor1922](https://github.com/asadman1523/QRCodeFor1922)
- 目前專案：[rsps1008/QRCodeFor1922](https://github.com/rsps1008/QRCodeFor1922)
- 聯絡方式：請透過目前專案的 [GitHub Issues](https://github.com/rsps1008/QRCodeFor1922/issues) 聯絡

## 應用程式處理的資料

- 相機影像：用於在裝置上辨識 QR Code／Aztec 條碼。應用程式不會將相機影像保存為掃描歷史。
- 掃描內容：辨識出的文字、網址、簡訊、電話、Email 或 Wi-Fi 內容會依功能保存於裝置本機的掃描歷史資料庫；你可以在 App 內刪除紀錄。
- 設定資料：例如外觀、是否自動開啟內容、是否自動複製文字等，保存於裝置本機設定。
- 剪貼簿：啟用自動複製或手動複製時，掃描內容會寫入 Android 系統剪貼簿。
- Google Drive 備份：你在設定頁登入 Google 並執行備份時，歷史紀錄、網址標題、內容類型、時間與最愛狀態會保存到你自己 Google Drive 的 `appDataFolder`；還原會以該備份替換本機歷史。

## 網路連線

掃描到網址時，應用程式可能在背景向該網址發出 HTTP/HTTPS 請求，以讀取頁面標題。該請求會由目標網站依其自身政策處理；應用程式不會把掃描歷史上傳至本維護者的伺服器。

若你啟用 Google Drive 備份，App 會透過 Google OAuth 授權存取你帳戶的應用程式資料區；App 不會要求或使用 Google Drive 中其他檔案的讀寫權限。備份資料會保留在你的 Google 帳戶中，直到你自行刪除或覆蓋它。

應用程式使用 Google ML Kit Barcode Scanning、Google Sign-In、Google Drive API、AndroidX CameraX 與 AndroidX Room 等第三方元件。其使用方式與授權資訊列於[第三方授權清單](https://github.com/rsps1008/QRCodeFor1922/blob/main/THIRD_PARTY_LICENSES.md)。

## 外部應用程式與系統功能

依你的設定與掃描內容，App 可能要求 Android 系統或其他 App 開啟網址、簡訊、電話、Email 或 Wi-Fi 設定。這些外部服務由各自的提供者負責資料處理。

## 權限

App 可能使用相機、網路、Wi-Fi 狀態／設定與震動權限。相機只用於掃描；Wi-Fi 內容會交由 Android 系統的加入網路流程處理，不由 App 靜默連線。

## 資料刪除

你可以在歷史紀錄頁刪除掃描資料，也可以透過 Android 系統的 App 資料清除功能刪除本機資料。解除安裝 App 也會移除其本機資料，但 Android 系統或其他外部 App 已經取得的資料不在此範圍內。

## 政策變更

若資料處理方式有重大變更，我們會更新本頁的最後更新日期。繼續使用 App 即表示你已閱讀更新後的政策。

## 開源專案說明

本專案源自 [asadman1523/QRCodeFor1922](https://github.com/asadman1523/QRCodeFor1922)。原始 README 的版權與 Apache License 2.0 資訊已保留；目前版本的新增與修改部分採 MIT License。這是軟體授權資訊，不代表原作者為目前版本背書或提供支援。
