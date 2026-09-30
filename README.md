# A9
Android Application Dialer app

## Apie

A9 – greitos programų paieškos „pop-up“ langas Android 16+ (T9 numpad, neseniai naudotos programos, kontaktai, icon pack). Kotlin, Jetpack Compose, minSdk 29, targetSdk 36.

Būsena: [PROGRESS.md](PROGRESS.md).

## Kompiliavimas kitame kompiuteryje

Reikia:
- **JDK 17+** (Android Studio jį atsineša: `Settings → Build Tools → Gradle → Gradle JDK`).
- **Android SDK** su platforma **37** (`compileSdk = 37`) ir build-tools. Paprasčiausia – atidaryti projektą Android Studio, jis sukurs `local.properties` ir pasiūlys atsisiųsti trūkstamus komponentus. Iš komandinės eilutės užtenka nustatyti `ANDROID_HOME` (arba sukurti `local.properties` su `sdk.dir=/kelias/iki/Android/Sdk`) ir sutikti su licencijomis (`sdkmanager --licenses`).
- Gradle atsisiunčia pats per wrapper (`./gradlew`), papildomai nieko diegti nereikia.

```
./gradlew testDebugUnitTest assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk` (jei projektas OneDrive aplanke, build išvestis nukreipiama į `%LOCALAPPDATA%\A9-build\app`).

Diegimas į telefoną (USB debugging): `adb install -r app-debug.apk`, tada `adb shell am start -n lt.tbu.a9/.ui.DialerActivity`.

`local.properties` į git nekeliamas (jame lokalus SDK kelias).
