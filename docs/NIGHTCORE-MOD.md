# Outify Nightcore Mod

A fork of [iTomKo/Outify](https://github.com/iTomKo/Outify) with nightcore-style playback
speed and a few playback fixes.

## Changes

### Nightcore playback speed
Upstream Outify uses Media3's `SonicAudioProcessor`, which changes speed while keeping the
pitch (time-stretch). This mod removes Sonic and *resamples* instead, so tempo and pitch
rise together.

`playback/AudioEngine.kt`:
- **Preferred path**: `AudioTrack.setPlaybackRate(sampleRate * speed)`. Android's own
  resampler does the work. It sounds good and costs no CPU or allocations.
- **Fallback**: some devices reject rates above ~2x their native output rate. In that case a
  software linear-interpolation resampler is used. It keeps its fractional position and
  the last frame between PCM chunks so there are no clicks at chunk boundaries, and it reuses
  its output buffer.

`playback/Player.kt` reports `PlaybackParameters(speed, pitch = speed)` to media controllers.

### Speed slider
`ui/screens/player/PlaybackSpeedControl.kt` replaces the preset buttons with an
always-visible slider on the bottom of the album art:
- range 0.5x – 3x, snapping to 0.05 steps (and to exactly 1x when close)
- speed changes live while dragging
- tap the speed label to reset to 1x

### Speed is remembered
The speed is stored in DataStore (`playback_speed` in `SettingsRepository`). `Player`
restores it on startup and saves every change, whether it comes from the slider or a
media controller.

### Outify is the default playback device
librespot ignores Load/Play commands while another Spotify Connect device (PC, speaker,
web player) is the active device:

```
SpircCommand::... will be ignored while Not Active
```

Upstream's "smart transfer" deliberately doesn't take over on startup when another device
is active. So tapping a song did nothing until Outify was picked manually under *Playback
devices*. `SpircWrapper.makeThisDeviceActive()` now activates Outify before every
user-started playback (`load`, `shuffleLoad`, `localLoad`, `setQueue`). Just opening the
app still doesn't take playback away from another device.

### Fix: ANR kill while waiting for playback
`SpircWrapper` started `PlaybackService` with `startForegroundService()`. Media3 only calls
`startForeground()` once audio actually plays. If playback never started (for example, no
device selected), Android killed the app after the foreground-service timeout:

```
ANR in cc.tomko.outify
Reason: Context.startForegroundService() did not then call Service.startForeground()
```

The service is now started with a plain `startService()` while the app is visible. Only
if Android refuses that (background start) is `startForegroundService()` used. In that
case `PlaybackService.onStartCommand` immediately satisfies it with a short-lived
placeholder notification.

### Playback stability fixes
- **Random playback stops:** `PlaybackService` requested audio focus on its own, next to
  `Player`'s Media3 `AudioFocusManager`. Android tracks focus per listener, so when the
  service started during playback, the player got `AUDIOFOCUS_LOSS` and paused. The
  service no longer requests focus.
- **Transport controls while another device is active:** play/next/previous/seek
  first transfer the Connect session here. librespot ignores them otherwise, and the
  native watchdog then restarted the runtime after 30 s.
- **Activation:** `Activate` is always sent before user-started playback instead of
  trusting a possibly stale active-device flag.
- **Background start:** a refused `startForegroundService()` (Android 12+) is caught
  instead of crashing.
- **High speeds:** the `AudioTrack` buffer is sized for the maximum speed, avoiding
  underruns at 3x.
- **Speed saving:** a speed changed while the setting loads isn't overwritten, and saves
  are debounced while dragging.

## Building on Windows

Prerequisites:
- JDK 17 (e.g. Eclipse Adoptium Temurin 17)
- Android SDK with NDK 29.x (Android Studio's SDK manager)
- Visual Studio 2022 with the C++ workload (MSVC linker, needed by Rust build scripts)
- Rust via rustup (`winget install Rustlang.Rustup`). The toolchain version and Android
  targets are pinned in `rust/rust-toolchain.toml` and installed automatically.
- `cargo install cargo-ndk`

### 1. Clone
```bash
git clone --recurse-submodules https://github.com/danielwoww/Outify-Nightcore-Mod
cd Outify-Nightcore-Mod
```

### 2. Spotify credentials
Copy `keystore.properties.example` to `keystore.properties` and fill in your Spotify app's
client ID and secret (see [CONTRIBUTING.md](./CONTRIBUTING.md)). This file is gitignored,
so never commit it.

### 3. Build the native librespot library
From Git Bash:
```bash
export ANDROID_NDK_HOME="$LOCALAPPDATA/Android/Sdk/ndk/<29.x version>"
cd rust/librespot-ffi
cargo ndk -t arm64-v8a -t armeabi-v7a --platform 21 -o ../../app/src/main/jniLibs build --release
cd ../..
```
This produces `app/src/main/jniLibs/{arm64-v8a,armeabi-v7a}/liblibrespot_ffi.so`.

> `build-librespot.ps1` does the same, but under Windows PowerShell 5.1 it aborts on
> cargo's normal progress output (`$ErrorActionPreference = "Stop"` treats stderr as an
> error). Use the command above or PowerShell 7.

### 4. Build the APK
```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.<version>-hotspot"
./gradlew assembleDebug
```
APKs land in `app/build/outputs/apk/debug/` (`arm64-v8a` for most phones, plus
`armeabi-v7a` and `universal`).

If Gradle fails with `java.io.IOException: Unable to establish loopback connection`, the
JDK can't create its local socket in your temp folder (this happens with non-ASCII
characters in the Windows user name). Point it at an ASCII path:
```bash
mkdir -p .gradle/tmp
export JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=$(pwd -W)/.gradle/tmp"
```

`gradle.properties` sets `org.gradle.java.installations.auto-download=false`, so Gradle
uses your installed JDK 17 instead of downloading toolchains.

### 5. Install
```bash
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```
Debug builds are signed with your local debug key (`~/.android/debug.keystore`). Keep using
the same PC/key, or Android will refuse to update over an existing install.

### 6. Release build
`./gradlew assembleRelease` builds minified APKs in `app/build/outputs/apk/release/`.
They are signed with a release key when these environment variables are set. Otherwise
they fall back to the debug key:

```bash
ANDROID_KEYSTORE_FILE=C:/path/to/release.jks
ANDROID_KEYSTORE_PASSWORD=...
ANDROID_KEY_ALIAS=...
ANDROID_KEY_PASSWORD=...
```

Keep these values in a file outside the repository and load it before building:
```bash
set -a; . /c/path/to/signing.env; set +a
./gradlew assembleRelease
```

Create a key once with
`keytool -genkeypair -keystore release.jks -storetype PKCS12 -alias outify -keyalg RSA -keysize 4096 -validity 10000`.
Back up the keystore and its password. Without them, later releases can't update existing
installs. APKs signed with a different key than the installed app (e.g. debug vs.
release) can't update it in place, so the app must be uninstalled first.

Official releases of this fork are signed with a certificate whose SHA-256 is
`820b607080e4c077580b34505cc9ad6b9a28890b3b15d75a2655e5d7a70006e7`.
