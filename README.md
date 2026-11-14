# Black UI

In today's world, we have a lot of distractions. 
We have thousands of lucrative apps like social media apps, then come e-commerce or food delivery apps with their lucrative offers 24X7. 
We are spending a lot of time with phone and we somehow stopped thinking and being in present. 

This app turn the light off for those distraction. 

## Features

- Want to find your app? Swipe Left
- Want to search internet? Swipe up
- Want to make any app to your home page? Long Press the app
- Do you have any annoying app? Hide it

See [FEATURES.txt](FEATURES.txt) for interaction details and limitations.

## Local setup

1. Install Android Studio and configure **JDK 17** as the Gradle runtime.
2. Install the **Android SDK Platform 35** and Android SDK build/platform tools
   through Android Studio's SDK Manager.
3. Open this project in Android Studio and let Gradle sync. Android Studio can
   create `local.properties` with your local SDK path; this file is not committed.
4. Use an emulator or a deliberately selected test device running **API 28 or
   newer**.

## Build and validate

From the project root:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Run as a launcher

Run the `app` configuration from Android Studio on your selected emulator/device.
Opening the app icon displays the launcher UI. To use it for the device's Home
action, select it as the default home app through the device's settings or home
app chooser. Menu names vary by device.

The manifest advertises both a normal app entry (`MAIN + LAUNCHER`) and a home
entry (`MAIN + HOME + DEFAULT`). Installation alone does not make it the selected
default home app.

Open [launcher-guide.html](launcher-guide.html) locally in a browser for manifest
explanations, startup/action flowcharts, and the entry-point walkthrough. The
guide works offline; its external reference links require internet access.

## Make it a default Home launcher
1. Install the app
2. Open Settings -> Apps -> Default apps -> Home app
3. Select Balck UI.
4. Press Home button -> Your Black UI would be visible
