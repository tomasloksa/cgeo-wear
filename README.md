# cgeo-wear

A modern Wear OS compass companion for [c:geo](https://github.com/cgeo/cgeo) —
compass arrow + distance to the currently navigated cache on your watch.
Successor in spirit to the abandoned [cgeo/cgeo-wear](https://github.com/cgeo/cgeo-wear),
rebuilt from scratch for Wear OS 3+ with Kotlin, Compose for Wear OS (Material 3)
and the modern Data Layer APIs.

See [`WEAR_OS_INTEGRATION.md`](WEAR_OS_INTEGRATION.md) for the full research,
architecture rationale and roadmap.

## Modules

| Module | What |
|---|---|
| `wear/` | Watch app: Compose compass screen (ring, target arrow, distance) |
| `mobile/` | Phone bridge (M2): catches the `geo:` intent from c:geo's *Navigate with* menu, owns GPS, streams target + ticks to the watch |
| `common/` | Shared contract: `NavTarget`/`NavTick`, Data Layer paths, geo math |

Structured Locus-addon style so it can later migrate into the cgeo repo as a
`:wear` module (see cgeo [#7086](https://github.com/cgeo/cgeo/issues/7086)).

## Status

**M1** — watch UI running against a simulated walk (fake data), Wear OS
emulator. No phone bridge yet.

## Building

Requires JDK 17 and an Android SDK with platform 36.

```sh
./gradlew :wear:assembleDebug   # build the watch APK
./gradlew test                  # unit tests (geo math)
```

Run on a Wear OS emulator (round, API 36 wear image):

```sh
avdmanager create avd -n wear36 -k "system-images;android-36;android-wear;x86_64" -d wearos_large_round
emulator -avd wear36 &
./gradlew :wear:installDebug
adb shell am start -n io.github.tomasloksa.cgeowear/.MainActivity
```
