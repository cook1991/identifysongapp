# Identify Song

A small Android app (Kotlin + Jetpack Compose) that listens to whatever's playing nearby
and identifies the song, using the [AudD](https://audd.io/) audio-recognition API. Same
family as the sibling `smsmmsapp` / `policescanner` apps.

## How it works

1. Tap the button → the app records ~9 seconds of audio from the mic (`AudioRecorder`, raw
   `AudioRecord` PCM wrapped as a WAV file).
2. The WAV is uploaded to `https://api.audd.io/` (`AudDClient`), asking for extra
   `apple_music`/`spotify` metadata so we get cover art and a listen link back.
3. Result (title, artist, album, artwork, link) is shown; tap "Listen again" to retry.

No audio fingerprinting happens on-device — matching against a database of tens of millions
of tracks is what AudD (or Shazam/ACRCloud) provides as a service.

## One-time setup

1. **Get an AudD API token** — sign up free at https://dashboard.audd.io/, create an
   application, copy the token. Free tier is rate-limited (fine for personal use); paid
   tiers start around $2/mo for more requests.
2. Copy `local.properties.example` to `local.properties` (git-ignored) and fill in:
   ```
   sdk.dir=<your Android SDK path>
   AUDD_API_TOKEN=<your token>
   ```
   Android Studio auto-fills `sdk.dir` the first time it opens the project if missing.
3. Open the project folder in **Android Studio** (2024.2+ recommended, matching AGP 8.7.3 /
   Kotlin 2.0.21). Let it sync Gradle.
4. For release builds only: copy `keystore.properties.example` to `keystore.properties` and
   point it at your upload keystore. Debug builds don't need this.
5. Run on a **physical device** (recommended — emulator mics generally don't pick up real
   ambient audio well). Minimum Android 8.0 (API 26).

## Project layout

- `app/src/main/java/net/spritzlive/identifysong/audio/AudioRecorder.kt` — mic capture → WAV.
- `app/src/main/java/net/spritzlive/identifysong/network/AudDClient.kt` — multipart upload +
  response parsing.
- `app/src/main/java/net/spritzlive/identifysong/RecognitionViewModel.kt` — recording/
  identifying state machine.
- `app/src/main/java/net/spritzlive/identifysong/MainActivity.kt` — Compose UI + mic
  permission handling.

## Tech stack

- Kotlin + Jetpack Compose (Material 3), single activity
- `AudioRecord` for raw PCM capture, hand-rolled WAV header
- OkHttp for the multipart upload, `org.json` for response parsing (no SDK needed)
- Coil for album artwork
- Package `net.spritzlive.identifysong`; minSdk 26, compile/target 35

## Notes / things to tune later

- Recording length is fixed at 9s (`AudioRecorder.recordWav`'s `durationMs` default) — AudD
  generally matches on 5-10s clips.
- The `RECORD_AUDIO` runtime permission is requested on first tap; if denied, the UI shows a
  message but doesn't deep-link to Settings — add that if it becomes annoying.
- The AudD token is embedded in the APK's `BuildConfig` via `local.properties`. Fine for
  personal use; swap for a server-side proxy before shipping this beyond your own device,
  since the token is extractable from the built APK.
