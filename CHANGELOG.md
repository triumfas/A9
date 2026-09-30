# Changelog

All notable changes to A9 are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/), and the project uses [Semantic Versioning](https://semver.org/).

## [1.0.0] – 2026-09-30

First public release.

A9 is a fast app-search pop-up for Android 16+: a compact panel with a T9 keypad that opens on top of whatever you are doing, finds an app (or contact) in a few taps and offers the most useful actions on long-press. Everything runs locally – no internet permission, no account, no analytics.

### Added
- **T9 search** (3×3 keypad, digits 2–9) by name, words, `CamelCase` parts, initials, package name and substring; accented letters are matched as their base letters. Matched letters are highlighted in the app name.
- **Result pages** you can swipe sideways; configurable 3–6 columns × 1–3 rows.
- **Smart ordering** for an empty query: pinned → last launched via A9 → recently used on the phone (optional, via Usage access) → alphabetical.
- **Long-press menu** (App info, Pin, Add to Home screen, Hide, Play Store, Uninstall, Force stop) that opens next to the icon, with the most-used actions closest to it; A9 learns the order from your use.
- **Contacts** (optional): search by name or number, call, SMS, open.
- **Quick Launch notification** with your most-used apps.
- **Icon packs** (ADW / Nova / Go).
- **Appearance** with a live preview: Dark / Light / Transparent themes, custom background color (color wheel, brightness, transparency) and accent color.
- **Window position and size**: drag and resize the panel.
- **Hidden apps** screen with search.
- Haptic feedback option, English and Lithuanian UI, work-profile app support.
- Fast cold start: the app list and icons are cached on disk.

### Notes
- Distributed as an APK for sideloading (it uses `QUERY_ALL_PACKAGES`, which Google Play does not allow for this purpose).
- Requires Android 10+ (`minSdk 29`); built and tested for Android 16.
- Known limitations: the system “swipe up to Home” animation shrinks the whole translucent window; native per-app shortcuts and the real Recents list are not available to third-party apps.

### Verify the download
The release APK is signed with the key whose SHA-256 certificate digest is
`c057b281a164b4bf3f0d7adcefa4aa2d9bfbe895ed2cb2c190425e9456444520`.
The APK's own SHA-256 checksum is attached to the release as `A9-1.0.0.apk.sha256`.

[1.0.0]: https://github.com/triumfas/A9/releases/tag/v1.0.0
