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

**M2** — end-to-end companion. The phone bridge catches c:geo's `geo:` intent,
owns GPS, and streams the target (DataClient) + distance/bearing ticks
(MessageClient) to the watch. Requires a **paired** phone + watch.

## Building

Requires JDK 17 and an Android SDK with platform 36.

```sh
./gradlew :wear:assembleDebug     # build the watch APK
./gradlew :mobile:assembleDebug   # build the phone bridge APK
./gradlew :common:test            # unit tests (geo math, codec, geo: parsing)
```

## Running

Both apps must be signed with the same key (the debug keystore satisfies this)
and installed on a **paired** phone + watch.

```sh
./run-watch.sh    # build + install + launch the watch app (pairs on first run)
./run-phone.sh    # build + install the bridge, fire a test geo: intent, tail logs
```

`run-phone.sh` remembers the phone's adb serial in `.phone-serial`. Pass
`--no-trigger` to skip the synthetic `geo:` intent (e.g. when driving the flow
from c:geo itself).

To launch the watch app manually on an emulator:

```sh
adb shell am start -n io.github.tomasloksa.cgeowear/.MainActivity
```

## Logs / debugging

Stream both devices at once, colour-tagged per device:

```sh
./run-logs.sh          # filtered to this project's tags
./run-logs.sh --all    # unfiltered logcat from both devices
```

Or point `adb` at one device with `-s <serial>` (find serials with `adb devices`)
using the tags below.

**Phone (bridge):**

```sh
adb -s <phone> logcat -s NavigateActivity:D BridgeService:D
```

| Log line | Meaning |
|---|---|
| `target: <name> @ <lat>,<lon>` | intent parsed, service started |
| `target published to Data Layer` | DataClient accepted the target |
| `requesting location updates …` | FusedLocation subscribed |
| `tick -> <watch>: <m> <deg>` | a tick was sent to a connected node |
| `no connected nodes - is a watch paired?` | phone can't see the watch (pairing/app mismatch) |

**Watch:**

```sh
adb -s <watch> logcat -s DataLayerNav:D NavListenerSvc:D HeadingProvider:D
```

| Log line | Meaning |
|---|---|
| `registering Data Layer listeners` | compass screen is collecting |
| `target from initial data items` / `target via DataClient change` | target arrived |
| `tick via MessageClient …` | a live tick arrived |
| `not ready yet: target=…, tick=…` | still waiting — shows which half is missing |
| `emitting NavState …` | full state ready; the compass leaves "Waiting…" |

### "Waiting for target…" won't clear

The watch needs **both** a target *and* at least one tick before it shows the
compass. Check, in order:

1. Phone log shows `target published` and `tick -> <watch>` (not `no connected nodes`).
2. Watch log shows `target …` and `tick via MessageClient`. If you see
   `not ready yet: target=true, tick=false`, the phone has no GPS fix yet
   (move outdoors / set a location on an emulator).
3. If the phone shows `no connected nodes`, the phone and watch aren't paired,
   or the two APKs are signed with different keys / different app ids.
