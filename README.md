# MyMinimalistDesignHomePage

A minimalist Android home-screen launcher built with Kotlin and Jetpack Compose.
The UI has a clock, favorite apps, phone and camera shortcuts, and a searchable
app drawer.

## Features

- Swipe horizontally between the home screen and app drawer.
- Swipe up on home to request web search, with a browser fallback.
- Search installed apps by label, including local aliases.
- Long-press an app to add/remove a favorite, rename it, hide/unhide it, open app
  info, or request uninstall through Android's confirmation flow.
- Persist favorites, hidden state, and aliases using profile-specific app IDs.
- Enumerate and launch apps in available personal/work profiles, subject to
  Android's profile policies.

See [FEATURES.txt](FEATURES.txt) for interaction details and limitations.

## Local setup

1. Install Android Studio and configure **JDK 17** as the Gradle runtime.
2. Install the **Android SDK Platform 35** and Android SDK build/platform tools
   through Android Studio's SDK Manager.
3. Open this project in Android Studio and let Gradle sync. Android Studio can
   create `local.properties` with your local SDK path; this file is not committed.
4. Use an emulator or a deliberately selected test device running **API 28 or
   newer**.

The Gradle wrapper is included; a separate Gradle installation is not needed.
The project currently uses Gradle 8.11.1, Android Gradle Plugin 8.10.0, and Kotlin
2.0.21. Java/Kotlin compilation targets 11; the Gradle runtime uses JDK 17.
The first sync/build needs internet access to download missing dependencies.

## Build and validate

From the project root:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For device/UI tests, start a dedicated test emulator and explicitly select its
serial. Replace the example serial with the one shown by `adb devices`:

```bash
ANDROID_SERIAL=emulator-5556 ./gradlew :app:connectedDebugAndroidTest
```

Device tests install the app and instrumentation package. Do not target a
personal device unless you intend to install and test them there.

## Run as a launcher

Run the `app` configuration from Android Studio on your selected emulator/device.
Opening the app icon displays the launcher UI. To use it for the device's Home
action, select it as the default home app through the device's settings or home
app chooser. Menu names vary by device.

The manifest advertises both a normal app entry (`MAIN + LAUNCHER`) and a home
entry (`MAIN + HOME + DEFAULT`). Installation alone does not make it the selected
default home app.

## Code map

| File | Responsibility |
| --- | --- |
| `app/src/main/AndroidManifest.xml` | Activity entry points, package visibility, and uninstall-request permission |
| `app/src/main/java/com/lostfalcon/myminimalistdesignhomepage/MainActivity.kt` | `onCreate()` entry point, Compose screens, gestures, app actions, and profile-aware app loading |
| `app/src/main/java/com/lostfalcon/myminimalistdesignhomepage/LauncherPreferences.kt` | Favorites, aliases, and hidden-app persistence |
| `app/src/main/java/com/lostfalcon/myminimalistdesignhomepage/ui/theme/` | Compose colors and typography |
| `app/src/test/` | Local unit tests |
| `app/src/androidTest/` | Device/UI tests |
| `gradle/libs.versions.toml` | Dependency and plugin versions |

Open [launcher-guide.html](launcher-guide.html) locally in a browser for manifest
explanations, startup/action flowcharts, and the entry-point walkthrough. The
guide works offline; its external reference links require internet access.

## Repository contents

Commit the source, resources, Gradle wrapper (including `gradle-wrapper.jar`),
build configuration, tests, and documentation.

The ignore rules exclude local SDK paths, IDE state, caches, build outputs,
packaged apps, environment files, and common signing files. The local
`minimalistOriginal.apk` is a reference artifact, not part of the repository.

Keep credentials and release-signing material outside version control, including
for a private repository. Ignore rules do not detect secrets embedded in source
files or remove files that were already tracked.
