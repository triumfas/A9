# A9 – projekto būsena

## Kas tai
Greitos programų paieškos „pop-up“ langas Android 16+: T9 numpad, rezultatų puslapiai, neseniai naudotos programos, kontaktai, Quick Launch pranešimas, icon pack'ai, kompaktiškas long-press meniu, išvaizdos nustatymai.

## Technologijos
Kotlin 2.4, Jetpack Compose (Material 3), AGP 9.4, compileSdk 37, targetSdk 36, minSdk 29. Paketas ir applicationId: `lt.tbu.a9`.
Saugojimas: DataStore (nustatymai), JSON failas (naudojimo statistika, prisegtos/paslėptos), programų sąrašo ir ikonų talpykla diske. Paieška – tiesinis skenavimas (pakanka šimtams/tūkstančiams įrašų).

## Struktūra
- `search/` – T9 kodavimas, tokenizavimas, reitingavimas, paryškinimas (grynas Kotlin, su testais).
- `data/` – programų (LauncherApps), kontaktų, neseniai naudotų (Usage access), statistikos ir nustatymų saugyklos.
- `icons/` – ikonų įkėlimas (atmintis + diskas), icon pack'ai.
- `ui/` – plokštė, numpad, rezultatai, long-press meniu, nustatymai, išvaizda, paslėptos programos.
- `notify/`, `shortcut/` – Quick Launch pranešimas, paleidimas per shortcut'ą.

## Patikrinta
`./gradlew testDebugUnitTest assembleDebug` (21 testas) ir rankiniu būdu Samsung telefone (Android 16): paleidimas, T9 paieška, paryškinimas, long-press meniu, App info, Add to Home, išvaizdos nustatymai, neseniai naudotos programos, disko talpyklos.

## Dar netikrinta
Uninstall, Force stop, Play Store, Pin/Hide, Hidden apps ekranas, kontaktų paieška, Quick Launch pranešimas, icon pack'ai.

## Pastabos
- Kompiliavimas kitame kompiuteryje: žr. [README.md](README.md).
- Jei projektas guli OneDrive aplanke, build išvestis nukreipiama į `%LOCALAPPDATA%\A9-build\app` (OneDrive laužo `build`).
- „Swipe up į Home“ animacija – sistemos elgsena viso ekrano permatomam langui; sprendimas: palikti.
