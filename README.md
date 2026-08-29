# QR Code Scanner

簡單、快速、以隱私為優先的 Android QR Code 掃描器。

官方網站：<https://qrcode.rsps1008.ru/>

## 功能

- 掃描 QR Code 與 Aztec 條碼
- 支援文字、網址、簡訊、電話、Email 與 Wi-Fi 內容
- 掃描歷史與我的最愛
- 網址可在背景取得頁面標題，原始網址仍會完整保存
- 可選擇自動開啟網址、簡訊、電話、Email 或 Wi-Fi 系統設定
- 可選擇自動複製文字與震動提示
- 支援明亮／深色外觀

掃描歷史資料儲存在裝置本機。完整資料處理方式請參閱[隱私權政策](https://qrcode.rsps1008.ru/privacy-policy/)。

## 開源來源與致謝

本專案是從原作者 [asadman1523/QRCodeFor1922](https://github.com/asadman1523/QRCodeFor1922) 延續與改寫的版本。原始專案 README 宣告採用 Apache License 2.0，並包含 `Copyright (C) 2021 YuJhen`；相關原始資訊仍予以保留。

目前維護版本：[rsps1008/QRCodeFor1922](https://github.com/rsps1008/QRCodeFor1922)

## 第三方元件

- [Google ML Kit Barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning)
- [AndroidX CameraX](https://developer.android.com/media/camera/camerax)
- [AndroidX Room](https://developer.android.com/training/data-storage/room)

各元件的授權與版權資訊請參閱 [`THIRD_PARTY_LICENSES.md`](./THIRD_PARTY_LICENSES.md)。

## 建置

需求：Android Studio、JDK 21、Android SDK 34。

Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleDebug
```

## 授權

本專案新增與修改的部分採用 [MIT License](./LICENSE)。源自原始專案的程式碼與其版權聲明仍依 [Apache License 2.0](./LICENSE-APACHE-2.0) 處理；散布衍生版本時請同時保留兩份授權與原作者資訊，並在修改過的檔案中標示修改內容。

請勿使用原作者名稱、商標或產品識別，使使用者誤以為本版本是原作者官方版本。

## 網站

網站原始檔位於 [`docs/`](./docs/)，GitHub Pages 網域由 `docs/CNAME` 設定為 `qrcode.rsps1008.ru`。網站包含[首頁](https://qrcode.rsps1008.ru/)、[隱私權政策](https://qrcode.rsps1008.ru/privacy-policy/)與[授權說明](https://qrcode.rsps1008.ru/license/)。
