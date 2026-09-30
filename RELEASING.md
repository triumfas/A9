# Releasing A9

How to build, sign and publish a release APK, and how to set up signing on another computer.
This file contains **no secrets**: the signing key and its passwords never live in this repository.

## Overview

| Thing | Where it lives | In git? |
|---|---|---|
| Source code, Gradle wrapper | this repository | yes |
| Signing key (`a9-release.jks`) | outside the repo, e.g. `~/a9-signing/` | **never** |
| Key alias and passwords | your **user-level** `~/.gradle/gradle.properties` | **never** |
| Release APK | `app/build/outputs/apk/release/app-release.apk` | no (attach to a GitHub release) |

Android only lets an installed app be updated by an APK signed with the **same key**. Losing the key means users have to uninstall and reinstall A9 (and lose its settings), and a new key with the same identity can never be recreated. Back it up.

## 1. Bump the version

In `app/build.gradle.kts`:

```kotlin
versionCode = 2        // must increase with every release
versionName = "1.1.0"  // human-readable, semantic versioning
```

Update `README.md` if features changed, commit, then tag the release (see step 5).

## 2. Set up signing (once per computer)

Without this step the release build still works, but it is signed with the **debug** key: fine for testing, but such an APK cannot update an install signed with the real release key.

1. Copy `a9-release.jks` to a folder **outside** the repo and outside cloud-synced folders, for example:
   - Windows: `C:\Users\<you>\a9-signing\a9-release.jks`
   - Linux / macOS: `~/a9-signing/a9-release.jks`
2. Create or extend your **user-level** Gradle properties file (**not** the one in the project):
   - Windows: `C:\Users\<you>\.gradle\gradle.properties`
   - Linux / macOS: `~/.gradle/gradle.properties`

   ```properties
   A9_STORE_FILE=C:/Users/<you>/a9-signing/a9-release.jks
   A9_STORE_PASSWORD=<keystore password>
   A9_KEY_ALIAS=a9
   A9_KEY_PASSWORD=<key password>
   ```

   Use forward slashes in the path, even on Windows. If the file already exists, **append** these lines instead of overwriting it.

   *Alternative for CI:* set the environment variables `ORG_GRADLE_PROJECT_A9_STORE_FILE`, `ORG_GRADLE_PROJECT_A9_STORE_PASSWORD`, `ORG_GRADLE_PROJECT_A9_KEY_ALIAS` and `ORG_GRADLE_PROJECT_A9_KEY_PASSWORD`.

`app/build.gradle.kts` reads exactly these four properties. If `A9_STORE_FILE` is missing it silently falls back to the debug key, so anyone can still build the project.

## 3. Build

```
./gradlew testDebugUnitTest assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk` (shrunk with R8). If the project sits in a OneDrive folder the output is redirected to `%LOCALAPPDATA%\A9-build\app\outputs\apk\release\`.

## 4. Verify

Check that the APK is signed with the intended key (`apksigner` is in `<Android SDK>/build-tools/<version>/`):

```
apksigner verify --print-certs app-release.apk
```

The SHA-256 certificate digest of the A9 release key is:

```
c057b281a164b4bf3f0d7adcefa4aa2d9bfbe895ed2cb2c190425e9456444520
```

If the digest differs, the wrong key (or the debug key) was used. Do not publish that APK.

Then install it on a device and smoke-test it. Installing over a build signed with another key fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`; uninstall the old build first.

```
adb install -r app-release.apk
adb shell am start -n lt.tbu.a9/.ui.DialerActivity
```

## 5. Publish on GitHub

```
git tag -a v1.1.0 -m "A9 1.1.0"
git push origin main v1.1.0
```

On GitHub: **Releases → Draft a new release**, pick the tag, attach the APK (rename it, e.g. `A9-1.1.0.apk`) and its checksum file:

```
sha256sum A9-1.1.0.apk > A9-1.1.0.apk.sha256
```

## Keeping the key safe

- Keep **two** copies of `a9-release.jks` plus the four property values: one in a password manager (most support file attachments) and one offline (USB stick). Store the passwords separately from the keystore file.
- **Never** commit them, put them in a cloud-synced project folder, e-mail them, or paste them into issues or chat.
- Do not print the passwords in CI logs; use encrypted CI secrets.
- The keystore was created with RSA 4096 and a validity of about 30 years, so it does not need to be renewed.
- If you ever suspect the key leaked, publish a new release under a **new** key and tell users to reinstall; there is no way to revoke the old one.
