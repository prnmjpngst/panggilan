# Pemanggilan

Android front-end + backend for playing vehicle inspection announcements over loudspeaker.

## Parts
- `backend/` Node/Express service (port 8050), exposed at https://prnmjpngst.pw/pemanggilan
- `android/` Native Android app (Kotlin + Compose)
- `apps-script/poll.gs` Google Sheets poller

## Backend run
```
cd backend
cp .env.example .env  # edit API_KEY
npm install
npm start
```

## Android build
GitHub Actions builds debug APK on push. Or locally with Gradle 8.9:
```
cd android
gradle assembleDebug
```

## Apps Script
Paste `apps-script/poll.gs` into the sheet's Apps Script, set triggers (onEdit + every minute).

## API key
Sample key in repo: `panggilan-dev-key-2026-change-me`. Change for production in backend `.env`, Apps Script, and app Config.