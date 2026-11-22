# Weekly Permit Report — Offline Android App (GASCO · Amerya LPG Plant)

100% offline Android app built with **Kotlin, Jetpack Compose, Room SQLite Database, and on-device ML Kit OCR**, bundled with your exact **GASCO `cover.png`** drawable at `app/src/main/res/drawable/cover.png`.

- **Zero Internet Permission**: All permits and reports stay locally on your phone.
- **Room Database (`gasco_permit_report.db`)**: Persists `permit_reports` and `permit_activities` partitioned into **1. PM**, **2. CM**, **3. Communication (COMM)**, and **4. Engineering (ENG)** sorted chronologically with continuous `01..N` numbering.
- **Exact 2-Page Offline Exports**: Generates the exact 2-page GASCO **PDF** (`PdfDocument`), **`.tex`** LaTeX file, and **`cover.png`** directly on-device.

## Option 1: Build .apk on GitHub Actions (No Android Studio needed)
1. Unzip and push this folder to your GitHub repo.
2. Open **Actions -> Build GASCO Weekly Permit APK -> Run workflow**.
3. Download `gasco-weekly-permit-report-apk` and install `app-debug.apk` directly on your Android phone.

## Option 2: Build locally in Android Studio
1. Open the unzipped folder in **Android Studio Ladybug (2024.2+)**.
2. Select **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
3. Sideload `app/build/outputs/apk/debug/app-debug.apk` onto your personal Android phone.
