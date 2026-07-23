# Wear OS integration for c:geo — research, options & proposal

*Status: discussion document / draft — July 2026*

**Goal (v1):** a simple, battery-friendly Wear OS screen showing a **compass arrow + distance to the currently navigated cache**, launched from c:geo's navigation menu.

---

## 1. History: why the old app is dead

There *was* a companion app — **cgeo-wear** by cjmoran/"javadog" ([cjmoran/cgeo-wear](https://github.com/cjmoran/cgeo-wear), later adopted into the org as [cgeo/cgeo-wear](https://github.com/cgeo/cgeo-wear), newest fork [ztNFny/cgeo-wear](https://github.com/ztNFny/cgeo-wear)). Archived read-only in Feb 2025, "no longer supported".

How it worked: c:geo fired an intent `cgeo.geocaching.wear.NAVIGATE_TO` → an exported `Service` in the companion phone app caught it → a **permanent foreground service with continuous GPS** streamed distance/bearing to the watch over the **Wear 1.x-era `GoogleApiClient` / `MessageApi`** stack.

Why it can't be revived:

- The entire API surface (`GoogleApiClient`, `MessageApi`, `BIND_LISTENER` binding) is deprecated/removed on modern Wear OS 3+.
- The architecture itself is the battery problem: always-on foreground service, continuous high-accuracy GPS, unthrottled sensor listeners, chatty Bluetooth message streaming. These are today's documented Wear anti-patterns.
- Maintainer verdict (ztNFny, [#12920](https://github.com/cgeo/cgeo/issues/12920), 2022): *"the codebase is no good, it'd need a full rewrite. The current one is consuming way too much battery to be useful for anything."*

### Timeline in the c:geo repo

| Issue/PR | What | Status |
|---|---|---|
| [#4063](https://github.com/cgeo/cgeo/issues/4063) (2014) | Original "Add support for Android Wear" FR | closed |
| [#4417](https://github.com/cgeo/cgeo/pull/4417) (2014) | **Merged**: send-side hook — the `NAVIGATE_TO` intent + service detection | merged |
| [#6515](https://github.com/cgeo/cgeo/issues/6515) (2017) | Remove c:geo wear from recommended apps (companion de-published) | closed |
| [#6557](https://github.com/cgeo/cgeo/pull/6557) (2017) | **Merged**: wear hook removed from c:geo — *no wear code remains in c:geo today* | merged |
| [#7086](https://github.com/cgeo/cgeo/issues/7086) (2018) | Main "Wear support" tracking issue | closed **not planned** (2023) |
| [#12920](https://github.com/cgeo/cgeo/issues/12920) (2022) | Smartwatch support (dup of #7086) | open |
| [#16572](https://github.com/cgeo/cgeo/issues/16572) (2025) | Wear OS app request | closed as dup |

### Maintainer sentiment — the door is open

The "not planned" closures reflect lack of bandwidth, not rejection:

- **Lineflyer** ([#7086](https://github.com/cgeo/cgeo/issues/7086)): *"getting back support for Android wear is interesting for me… independently whether it is a dedicated app or a separate addon it would be desirable for me to have it in this repository under open-source license."*
- **Bananeweizen** ([#6515](https://github.com/cgeo/cgeo/issues/6515)): described a concrete acceptance path — fork under the cgeo org → refactor → potentially move into the main repository.
- **kbudde** ([#7086](https://github.com/cgeo/cgeo/issues/7086), 2019): attempted a rewrite, got a working demo, gave up on the old codebase: *"rewriting the whole project would make sense."*

**Conclusion:** a credible, modern, low-battery rebuild would very likely be welcomed under the cgeo org — but the send-side hook must be re-added to c:geo (it was stripped in 2017).

---

## 2. Architecture options

| | **A) `:wear` module in cgeo repo** | **B) Companion pair (bridge app + wear app)** | **C) Fully standalone watch app** |
|---|---|---|---|
| How it works | Phone-side code lives in c:geo itself; wear APK ships under the **same package name**; Play auto-delivers it to paired watches | Modernized old model: c:geo → intent → small phone bridge app → Data Layer → watch | Watch has own GPS + cache data; works without phone |
| c:geo changes | Yes: wear Gradle module + "navigate on watch" hook | **None for a prototype** (see §3), tiny PR later | None, but needs its own geocaching.com API access |
| User experience | Best: one install, one button | Two extra installs | Best offline; heaviest to build |
| Team coordination | Full buy-in required upfront | Zero to prototype, buy-in only for adoption | n/a |
| Risk | Blocked on maintainer decisions before anything runs | Might stay a third-party app forever | Scope far beyond v1 |

### Recommendation

**Prototype as B, structured to migrate to A.** Build the demo as a separate project with the module layout that Locus Map's actively-maintained wear add-on uses ([asamm/locus-addon-wearables](https://github.com/asamm/locus-addon-wearables) — the best living reference for this exact pattern):

```
mobile/   ← phone bridge: receives target from c:geo, owns GPS, computes distance/bearing
wear/     ← watch UI: Compose compass screen
common/   ← shared data contract (message paths, serialized payloads)
```

This gets a working demo with zero dependency on the c:geo team, and if they adopt it, `mobile/` logic moves into cgeo's app module, `wear/` + `common/` become cgeo Gradle modules, and the standalone bridge app is retired. Option C (standalone watch GPS) stays on the table as a v2 config switch — the design below keeps the location source pluggable.

---

## 3. Integration with c:geo

### Prototype: zero c:geo changes needed

c:geo's **"Navigate with" menu already broadcasts to any app** that registers for `geo:` URIs. `OtherMapsApp` sends:

```
ACTION_VIEW  geo:<lat>,<lon>?q=<url-encoded cache name>
```

A bridge app with an `ACTION_VIEW` + `scheme="geo"` intent filter shows up in that menu automatically. That's the demo trigger — no fork, no PR, no waiting.

### Proper hook (the eventual c:geo PR), two candidates

1. **Re-add a dedicated intent** like the old `cgeo.geocaching.wear.NAVIGATE_TO` ([PR #4417](https://github.com/cgeo/cgeo/pull/4417) is the precedent, including service-detection in `ProcessUtils.isIntentAvailable()`). Clean, explicit, shows a "Wear OS" entry only when the companion is installed.
2. **Reuse the Radar-style contract**: `AbstractRadarApp` + `AbstractPointNavigationApp.addIntentExtras()` already send `latitude`, `longitude`, `name`, `code`, `difficulty`, `terrain`, `size` as extras (the Pebble app used exactly this). Minimal new code in c:geo.

Either is a small, low-risk PR to `main/src/main/java/cgeo/geocaching/apps/navi/`.

---

## 4. Proposed v1 technical design

**Companion, phone-authoritative.** The phone owns GPS and cache data; the watch is a smart display plus its own orientation sensor. This is what every surviving app in this category does (Locus, WearGo, Geooh GO).

### Data flow

```
c:geo ──geo:/intent──▶ bridge (phone)                      watch
                        │ FusedLocationProvider fix         │
                        │ computes distance + bearing       │
                        ├─ DataClient: target cache ───────▶│  (durable, replays on reconnect)
                        ├─ MessageClient: {distanceM,       │
                        │   bearingDeg} at ≤1 Hz ──────────▶│  (cheap live tick)
                        └─ CapabilityClient ◀──────────────▶┘  (discovery: is the watch app there?)
```

- **`DataClient`** carries the authoritative navigation target (coords, cache name, geocode) — survives Bluetooth drops and replays when the watch reconnects.
- **`MessageClient`** carries the live `{distance, bearing-to-target}` tick — fire-and-forget, throttled to ≤1 Hz, no retry needed.
- **`CapabilityClient`** detects whether the watch app is installed/reachable; **`RemoteActivityHelper`** is used only to prompt "install the watch app" via Play.
- Watch app launch: bridge sends a message → `WearableListenerService` on the watch starts the compass activity (this is the sanctioned pattern; there is no general "start remote activity" API).

### Watch side

- **Kotlin + Compose for Wear OS + Horologist**, Material 3. Kotlin is effectively mandatory for modern wear UI; this is fine even though c:geo is Java — a wear Gradle module can be Kotlin-only.
- **Compass arrow = watch's own rotation-vector/magnetometer** (phone orientation is useless on a wrist) combined with phone-supplied bearing-to-target. **Fall back to GPS-course-derived heading while moving** — magnetometer jitter/disturbance is the single most common user complaint in this app category (Locus even ships a settings toggle for it).
- **Ongoing Activity API** wraps the navigation session so it stays reachable from the watch face, like a workout.
- Distance as large text; that's the whole v1 screen. Optional later: a Tile with distance-at-a-glance, a complication.

### SDK targets

| | minSdk | targetSdk | notes |
|---|---|---|---|
| `wear/` | 30 (Wear OS 3) | 34 | Play requires targetSdk 34+ since Aug 2025; below 30 buys almost no real devices and breaks Compose-for-Wear tooling |
| `mobile/` bridge | 26 | 34+ | matches c:geo's minSdk for painless later merge |

### Battery rules (the four things that killed the old app)

1. **No continuous GPS on the watch** — phone owns location in v1.
2. **Unregister sensor listeners** the moment the screen isn't interactive.
3. **No custom always-on/ambient rendering** — current Android guidance says most apps shouldn't; rely on system ambient. (If ever needed: `AmbientLifecycleObserver`, ≥85 % black pixels.)
4. **Throttle the radio** — send state changes, not streams; watch BT can be as slow as ~4 KB/s.

---

## 5. Distribution

- **Google Play:** Wear apps ship on a **dedicated Wear OS release track** (mandatory since 2023), as a separate APK/AAB versioned independently. Under option A the wear APK shares c:geo's package name and Play auto-installs it on paired watches. Manifest: `uses-feature android.hardware.type.watch`, `com.google.android.wearable.standalone=false` for v1 (Google validates this — a companion app marked standalone gets filtered).
- **F-Droid: there is no mechanism to deliver watch apps.** F-Droid can't push an APK to a paired watch; users would have to sideload via ADB (or tools like WearLoad). Since c:geo ships on F-Droid, this needs a team decision — realistic options are documented ADB sideload instructions, or making the wear app standalone-capable in v2 so it's at least self-sufficient once sideloaded.

---

## 6. Demo roadmap

| Milestone | Scope | Validates |
|---|---|---|
| **M1 — watch UI alone** | Wear-only app, hardcoded target, compass ring + distance text, fake location feed; runs on Wear OS emulator | Compose compass rendering, sensor fusion of magnetometer + bearing |
| **M2 — end-to-end companion** | Phone bridge with `geo:` intent filter (appears in c:geo's Navigate menu), real FusedLocation, Data Layer wiring; paired phone + watch emulators | Full data flow triggered from unmodified c:geo |
| **M3 — real hardware** | Sideload to a physical Wear OS 3+ watch; walk a real cache trail | Compass jitter in the field, battery over a ~1 h caching session, ambient/Ongoing Activity behavior |
| **M4 — team discussion** | Present demo + this document to c:geo maintainers (reopen [#7086](https://github.com/cgeo/cgeo/issues/7086) or new discussion) | Decide: migrate into cgeo repo as `:wear` module + send-side PR, vs. cgeo-org companion repo |

Development loop: emulator pair for daily work, real watch for compass/battery validation.

---

## 7. Open questions for the c:geo team

1. **In-repo `:wear` module vs. companion app under the cgeo org?** (Lineflyer preferred "in this repository"; Bananeweizen sketched org-repo-first.)
2. **F-Droid stance** — is ADB-sideload documentation acceptable, or does F-Droid parity block the feature?
3. **Which send-side contract** to re-add: dedicated `NAVIGATE_TO`-style intent, or the existing Radar-extras pattern?
4. **Kotlin in the repo** — the wear module realistically must be Kotlin + Compose; is that acceptable in the Java codebase?
5. **Long-term maintainership** — the old app died from abandonment; who co-maintains?

---

## 8. References

**c:geo issues/PRs:** [#4063](https://github.com/cgeo/cgeo/issues/4063) · [#4417](https://github.com/cgeo/cgeo/pull/4417) · [#6515](https://github.com/cgeo/cgeo/issues/6515) · [#6557](https://github.com/cgeo/cgeo/pull/6557) · [#7086](https://github.com/cgeo/cgeo/issues/7086) · [#12920](https://github.com/cgeo/cgeo/issues/12920) · [#16572](https://github.com/cgeo/cgeo/issues/16572)

**Prior art:** [cjmoran/cgeo-wear](https://github.com/cjmoran/cgeo-wear) ([usage guide](https://github.com/cjmoran/cgeo-wear/wiki/Usage-Guide)) · [cgeo/cgeo-wear](https://github.com/cgeo/cgeo-wear) · [ztNFny/cgeo-wear](https://github.com/ztNFny/cgeo-wear) · [asamm/locus-addon-wearables](https://github.com/asamm/locus-addon-wearables) ([changelog](https://github.com/asamm/locus-addon-wearables/blob/master/CHANGELOG.md)) · [ehcloninger/cgeo-gear](https://github.com/ehcloninger/cgeo-gear) (Samsung Gear/Tizen variant) · WearGo (`com.devkor.weargo`) · [Geooh GO](https://geooh-go.com/)

**Platform docs:** [Data Layer client types](https://developer.android.com/training/wearables/data/client-types) · [Data Layer overview](https://developer.android.com/training/wearables/data/overview) · [Standalone vs non-standalone](https://developer.android.com/training/wearables/apps/standalone-apps) · [Packaging & distribution](https://developer.android.com/training/wearables/packaging) · [Wear release tracks (Play Console)](https://support.google.com/googleplay/android-developer/answer/13295490) · [Location on Wear](https://developer.android.com/training/wearables/apps/location-detection) · [Power & battery](https://developer.android.com/training/wearables/apps/power) · [Always-on guidance](https://developer.android.com/training/wearables/always-on) · [Horologist](https://github.com/google/horologist) · [RemoteActivityHelper](https://developer.android.com/reference/androidx/wear/remote/interactions/RemoteActivityHelper) · [F-Droid wear discussion](https://forum.f-droid.org/t/android-wear-support/1940)
