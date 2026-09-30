# A9

**A9** – greitos programų paieškos „pop-up“ langas Android 16+ telefonams. Atsidaro kaip kompaktiška plokštė virš bet kurios programos, leidžia per kelis paspaudimus surasti ir paleisti programą (ar kontaktą), o ilgai palaikius ikoną pasiūlo dažniausiai naudojamus veiksmus.

Idėja paprasta: **T9 klaviatūra + rezultatai per vieną žvilgsnį**, be kategorijų, aplankų ir ilgo slinkimo per programų sąrašą. Viskas veikia vietoje telefone – be interneto, be paskyros, be analitikos.

<p align="center">
  <img src="docs/screenshots/01-search.png" alt="Paieška T9 klaviatūra" width="360">
  &nbsp;&nbsp;
  <img src="docs/screenshots/06-light-blue.png" alt="Šviesi tema su mėlynu akcentu" width="360">
</p>

## Turinys
- [Funkcijos](#funkcijos)
- [Ekrano nuotraukos](#ekrano-nuotraukos)
- [Kaip naudotis](#kaip-naudotis)
- [Kaip veikia paieška](#kaip-veikia-paieška)
- [Nustatymai](#nustatymai)
- [Leidimai ir privatumas](#leidimai-ir-privatumas)
- [Greitis](#greitis)
- [Architektūra](#architektūra)
- [Kompiliavimas](#kompiliavimas)
- [Testai](#testai)
- [Žinomi apribojimai](#žinomi-apribojimai)
- [Licencija](#licencija)

## Funkcijos

**Paieška ir rezultatai**
- T9 numpad (3×3): raidės ieškomos per skaitmenis 2–9, pvz. `225` → **Cal**endar, **Cal**culator.
- Ieškoma pagal pavadinimą, atskirus žodžius, `CamelCase` dalis, inicialus (`gm` → Google Maps), paketo pavadinimą ir bet kur pavadinime. Lietuviškos ir kitos diakritinės raidės traktuojamos kaip bazinės (`š` → `s`).
- Atitikusios raidės **paryškinamos akcento spalva** pavadinime – užklausos eilutės nereikia, matote, kodėl programa pasirodė.
- Rezultatai rikiuojami pagal atitikimo kokybę ir jūsų naudojimo istoriją; rezultatai puslapiuojami – stumkite į šoną, kad pamatytumėte daugiau.
- Tuščia užklausa: **prisegtos** → **paskutinė paleista per A9** → **neseniai naudotos telefone** (neprivaloma) → likusios pagal abėcėlę.
- Veikia su **darbo profilio** programomis (žymima 💼).
- Kontaktų paieška (neprivaloma): pagal vardą arba telefono numerį.

**Veiksmai ilgai palaikius ikoną**
- App info, Pin / Unpin, Add to Home screen, Hide, Play Store puslapis, Uninstall (ne sistemoms), Force stop (atidaro sistemos App info ekraną su „Force stop“).
- Kompaktiškas meniu atsidaro virš ikonos (arba po ja) – **dažniausiai naudojami veiksmai arčiausiai ikonos**; A9 pati išmoksta tvarką iš jūsų naudojimo.
- Kontaktams: skambinti, SMS, atidaryti kontaktą.

**Kita**
- **Quick Launch pranešimas** su 6 dažniausiai naudojamomis programomis (atsistato po perkrovimo).
- **Icon pack'ai** (ADW / Nova / Go formatai).
- **Išvaizda su gyva peržiūra**: tema (Dark / Light / Transparent), fono spalva (spalvų ratas + šviesumas) su permatomumu, akcento spalva, programų skaičius lange (3–6 stulpeliai × 1–3 eilutės).
- **Lango padėtis ir dydis**: tempkite langą kur norite, didinkite / mažinkite (išdėstymas prisitaiko).
- **Paslėptos programos** su paieška – paslėpkite bet kurią ar atslėpkite atgal.
- Haptinis atsakas (galima išjungti), lietuvių ir anglų kalbos.

## Ekrano nuotraukos

Nuotraukos apkarpytos iki pačios A9 (be telefono fono ir kitų programų).

<table>
  <tr>
    <td align="center"><img src="docs/screenshots/01-search.png" width="300"><br><sub>Paieška <code>225</code>: paryškintos atitikusios raidės, CLEAR klavišas (ilgai – nustatymai)</sub></td>
    <td align="center"><img src="docs/screenshots/02-longpress-menu.png" width="220"><br><sub>Ilgas palaikymas: kompaktiškas meniu, dažniausias veiksmas apačioje – arčiausiai ikonos</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/06-light-blue.png" width="300"><br><sub>Šviesi tema su mėlynu akcentu</sub></td>
    <td align="center"><img src="docs/screenshots/03-settings.png" width="220"><br><sub>Nustatymai</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/04-appearance.png" width="220"><br><sub>Išvaizda: gyva peržiūra, tema, spalvos, stulpeliai / eilutės</sub></td>
    <td align="center"><img src="docs/screenshots/05-color-wheel.png" width="220"><br><sub>Spalvų ratas akcento spalvai + šviesumo slankiklis</sub></td>
  </tr>
</table>

## Kaip naudotis

| Veiksmas | Rezultatas |
|---|---|
| Paleisti A9 (ikona / kitas būdas) | Atsidaro plokštė; rezultatai – paskutinės naudotos programos |
| Rinkti skaičius 2–9 | Rezultatai siaurėja, atitikusios raidės paryškinamos |
| Bakstelėti ikoną | Programa paleidžiama, A9 užsidaro |
| Ilgai palaikyti ikoną | Veiksmų meniu |
| **CLEAR** (trumpai) | Išvalo visą įvestą užklausą |
| **CLEAR** (ilgai) | Atidaro nustatymus |
| Stumti rezultatus į šoną | Kiti rezultatų puslapiai |
| Bakstelėti už plokštės | Uždaro A9 |
| Atgal gestas | Pirma išvalo užklausą, po to uždaro |

Klavišo „1“ ir „0“ nėra sąmoningai: T9 jų nenaudoja, todėl vietoje „1“ yra CLEAR / nustatymai, o vieta sutaupoma.

## Kaip veikia paieška

Paieška – gryna Kotlin logika (`search/`), be Android priklausomybių, todėl lengvai testuojama.

1. **Normalizavimas** (`T9Map`): mažosios raidės, be diakritikų (`ą→a`, `ł→l`, `ß→s` …). T9 žemėlapis: `2=abc 3=def 4=ghi 5=jkl 6=mno 7=pqrs 8=tuv 9=wxyz`.
2. **Ieškomi fragmentai** (`Tokenizer`) kiekvienai programai / kontaktui:
   - `FULL` – visas pavadinimas be tarpų;
   - `WORD` – kiekvienas žodis (įskaitant `CamelCase` dalis);
   - `INITIALS` – žodžių pirmosios raidės;
   - `PACKAGE` – paketo pavadinimo dalys;
   - `PHONE` – kontakto telefono numeriai.
3. **Užklausos režimas:** jei užklausa tik iš skaitmenų – lyginama su fragmento **T9 kodu**, kitaip – su tekstu.
4. **Rikiavimas** (`SearchEngine`): atitikimo tipo bazinis balas (visas pavadinimas > telefonas > žodis > inicialai > paketas > bet kur) + prefikso ilgis; prie jo pridedama naudojimo istorija (paleidimų skaičius iki 50, paskutinio naudojimo šviežumas per 30 dienų, prisegimas +5000). Lygiais atvejais – pagal pavadinimą.
5. **Paryškinimas** (`Highlighter`): tuo pačiu prioritetu suranda, kurie pavadinimo simboliai atitiko užklausą.

Paieška yra tiesinis skenavimas per atmintyje laikomą indeksą (šimtams–tūkstančiams įrašų užtrunka <1 ms).

## Nustatymai

Atidaromi **ilgai palaikius CLEAR**.

- **Išvaizda** – tema, fono spalva ir permatomumas, akcento spalva, stulpeliai ir eilutės, viršuje gyva peržiūra. Pasirinkus temą atstatomi pritaikyto fono nustatymai.
- **Icon pack** – pasirinkite įdiegtą ikonų paketą arba numatytąsias ikonas.
- **Contacts** – kontaktų paieška (paprašo READ_CONTACTS ir CALL_PHONE).
- **Quick Launch Panel** – nuolatinis pranešimas su dažniausiomis programomis (paprašo pranešimų leidimo).
- **Recently used apps** – rodyti telefone neseniai naudotas programas (nukreipia į „Usage access“ ekraną).
- **Haptic feedback** – vibracija paspaudus klavišą / ilgai palaikius.
- **Window position and size** – tempimas, dydžio keitimas, atstatymas.
- **Force refresh index** – priverstinai perkrauti programų ir kontaktų sąrašą.
- **Hidden apps** – paslėptų programų valdymas (filtrai „Hidden“ / „All apps“, paieška, „Show all“).
- **Clear usage statistics** – ištrina A9 paleidimų istoriją (kiek kartų / kada paleista per A9). Prisegtų ir paslėptų nelieta; „Recently used apps“ naudoja sistemos istoriją, todėl nesikeičia.

## Leidimai ir privatumas

| Leidimas | Kam reikalingas | Kada |
|---|---|---|
| `QUERY_ALL_PACKAGES` | Matyti visas įdiegtas programas | visada |
| `REQUEST_DELETE_PACKAGES` | Išdiegimo dialogas (Uninstall) | visada, tik kai spaudžiate |
| `READ_CONTACTS` | Kontaktų paieška | tik įjungus |
| `CALL_PHONE` | Skambinti tiesiogiai (kitaip atidaromas rinkiklis) | tik įjungus kontaktus |
| `POST_NOTIFICATIONS` | Quick Launch pranešimas | tik įjungus |
| `RECEIVE_BOOT_COMPLETED` | Atkurti Quick Launch po perkrovimo | visada |
| `PACKAGE_USAGE_STATS` | Neseniai naudotos programos (Usage access) | tik įjungus ir suteikus |

**Interneto leidimo nėra.** Nėra analitikos, reklamų, paskyrų ar debesies. Duomenys saugomi tik programos privačiame aplanke:
- nustatymai – DataStore;
- naudojimo statistika, prisegtos ir paslėptos programos – `usage.json`;
- programų sąrašo kopija – `apps_cache.json`;
- ikonų talpykla – `cache/icons/*.png`.

Kadangi `QUERY_ALL_PACKAGES` tokiam tikslui neatitinka Google Play politikos, A9 skirta diegti tiesiogiai (sideload).

## Greitis

Šaltas paleidimas po „force close“ turi rodyti rezultatus iškart:
- **programų sąrašas** išsaugomas diske ir po paleidimo nuskaitomas per kelias milisekundes, o `LauncherApps` sąrašas fone jį patikslina;
- **ikonos** išsaugomos PNG failais ir nuskaitomos iš disko, o ne perpiešiamos iš sistemos; talpykla nusenta, kai programa atnaujinama;
- **nustatymai** pirmam kadrui nuskaitomi sinchroniškai, kad nešokinėtų tema;
- sąrašas atsinaujina automatiškai (`LauncherApps.Callback`), atnaujintų programų ikonos perkraunamos tik joms, be mirgėjimo.

## Architektūra

Kotlin 2.4, Jetpack Compose (Material 3), Gradle version catalog, AGP 9.4. `minSdk 29`, `targetSdk 36`, `compileSdk 37`. Paketas: `lt.tbu.a9`. Rankinis DI (vienas `AppContainer`), be Hilt.

```
app/src/main/java/lt/tbu/a9/
  A9App.kt                 Application + AppContainer (visos saugyklos)
  search/                  T9Map, Tokenizer, SearchEngine, Highlighter (gryna logika + testai)
  data/                    AppRepository (LauncherApps), ContactRepository, RecentAppsRepository,
                           UsageRepository (JSON), SettingsRepository (DataStore), Models
  icons/                   IconLoader (atmintis + diskas), IconPackManager (appfilter.xml)
  ui/                      DialerActivity, DialerScreen, DialerViewModel, keyboard/, results/,
                           actions/ (long-press meniu), settings/ (nustatymai, išvaizda, paslėptos), theme/
  notify/                  QuickLaunchNotifier, BootReceiver
  shortcut/                ShortcutTrampolineActivity (paleidimas per shortcut'ą / pranešimą)
```

Pagrindiniai sprendimai:
- **Lango forma:** permatomas viso ekrano `Activity`, kuriame Compose piešia plokštę; padėtis ir dydis – `BiasAlignment` + tankio (density) mastelis, todėl viskas mastelio keičiamas vienodai.
- **Paleidimas:** per `LauncherApps.startMainActivity` – veikia ir darbo profiliui.
- **Add to Home screen:** `ShortcutManagerCompat.requestPinShortcut` su `ShortcutTrampolineActivity`.
- **Sąrašo atnaujinimas:** `LauncherApps.Callback` vietoj manifesto `PACKAGE_*` transliacijų (jos nebegaunamos nuo Android 8).

## Kompiliavimas

Reikia:
- **JDK 17+** (Android Studio jį atsineša).
- **Android SDK** su platforma **37** ir build-tools. Paprasčiausia atidaryti projektą Android Studio (jis sukurs `local.properties` ir pasiūlys atsisiųsti trūkstamus komponentus). Iš komandinės eilutės užtenka nustatyti `ANDROID_HOME` (arba sukurti `local.properties` su `sdk.dir=…`) ir sutikti su licencijomis (`sdkmanager --licenses`).
- Gradle atsisiunčia pats per wrapper.

```
./gradlew testDebugUnitTest assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. (Jei projektas guli OneDrive aplanke, build išvestis nukreipiama į `%LOCALAPPDATA%\A9-build\app`, nes OneDrive užrakina failus.)

Diegimas į telefoną (USB debugging įjungtas):

```
adb install -r app-debug.apk
adb shell am start -n lt.tbu.a9/.ui.DialerActivity
```

Release versija (`assembleRelease`) naudoja R8 ir parašoma debug raktu – tinka asmeniniam naudojimui.

## Testai

`./gradlew testDebugUnitTest` – 21 vienetinis testas (`search/`): T9 kodavimas, diakritikos, tokenizavimas, reitingavimas pagal naudojimą ir prisegimą, paryškinimo indeksai (prefiksas, žodis, inicialai, `CamelCase`, bet kur pavadinime).

## Žinomi apribojimai

- **„Swipe up į Home“ animacija:** langas yra viso ekrano permatomas, todėl sistemos animacija sumažina visą langą (panelė slenka į ekrano vidurį). Tai sistemos elgsena – programa jos išjungti negali.
- **„Native“ programų shortcut'ai** (pvz. „New chat“) ilgai palaikant: Android juos duoda tik numatytajam paleidėjui (launcher), todėl A9 jų nerodo. Vietoje to yra App info.
- **Tikras Recents sąrašas** trečiosioms programoms neprieinamas; „Recently used apps“ naudoja sistemos naudojimo istoriją (Usage access) – labai panašu, bet ne identiška.
- **Force stop** negali sustabdyti kitos programos tiesiogiai – atidaroma sistemos App info su „Force stop“ mygtuku.

## Licencija

GNU General Public License v3.0 – žr. [LICENSE](LICENSE).
