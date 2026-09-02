# Mango TV

A streaming interface for Amazon Fire TV, built to feel like a piece of
consumer electronics rather than an app: cinematic, restrained, and fast on the
cheapest stick someone might own.

Kotlin, Views and RecyclerView, no Compose. Runs entirely on bundled mock
content — no network, no accounts, no backend.

---

## Building

```
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. Install it on a Fire TV Stick
over ADB:

```
adb connect <stick-ip>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`minSdk` is 22, which covers Fire OS 5 through current Fire OS 7 sticks. The
manifest declares both `LAUNCHER` and `LEANBACK_LAUNCHER`, so the app appears
on Fire OS and Android TV alike.

### Why CI builds this, not the machine it was written on

This project was developed in an environment whose egress policy denies
`dl.google.com`. That single block takes out the Android SDK, the Android
Gradle Plugin, AndroidX, Media3 and everything else on Google's Maven — so the
project could not be compiled locally at any point.

Every commit is therefore built on GitHub Actions
(`.github/workflows/android.yml`), which runs `assembleDebug` and `lintDebug`
and publishes the debug APK as a downloadable artifact. If you are building on
a machine with normal network access, the Gradle commands above just work.

**What this means for trust in the code:** CI proves it compiles and passes
lint. It has never run on Fire TV hardware, and no emulator was available. How
the focus animation actually feels, whether rows hold 60fps, and what cold
start costs are all unverified. `docs/PERFORMANCE.md` carries the checklist to
measure against and says the same thing.

---

## How it is put together

One Gradle module, with boundaries kept by package and interface:

```
app/src/main/java/tv/mango/app/
├── models/       Domain types. No Android, no serialisation, no bitmaps.
├── data/
│   ├── provider/ The seam: CatalogProvider, MovieProvider, SeriesProvider,
│   │             SearchProvider, StreamProvider. Return outcomes, never throw.
│   ├── mock/     Bundled content behind those interfaces.
│   └── local/    Watchlist and playback positions, on DataStore.
├── repository/   Provider results mapped to Loading / Content / Empty / Error.
├── cache/        Image pipeline. Every sizing and decode rule lives here.
├── ui/
│   ├── core/     The focus engine and the design system.
│   ├── home/     Cinematic hero over content rows.
│   ├── browse/   Movies and TV Shows grids.
│   ├── detail/   Film and series detail, cast, seasons, episodes.
│   └── common/   Error, empty and placeholder states.
├── navigation/   One activity, fragments, a rail, and Back.
├── di/           A hand-written object graph. No annotation processor.
├── player/       Empty. Phase 8.
└── settings/     Empty. Phase 7.
```

The interface named `CatalogProvider` rather than `ContentProvider` on purpose:
the latter is an Android framework class, and shadowing it would make every
import site ambiguous.

### Pointing it at a real backend

The screens never learn where content came from. Implement `CatalogProvider`
and the detail providers against a real service, then change the two lines in
`di/AppGraph.kt` that name `MockCatalogProvider` and `MockDetailProvider`.
Nothing above that file changes.

Artwork works the same way: models carry keys like `poster_m03`, and
`cache/ArtworkSource.kt` resolves a key plus a target size into something to
fetch. The bundled implementation returns an asset path; a remote one would
return a URL with the size folded into it, so the network delivers a
poster-sized image rather than a full-resolution one to shrink on the device.

### Regenerating the bundled content

Both scripts are deterministic and their output is committed, so they only need
running when the content or the look changes. They need Pillow and numpy.

```
python3 tools/generate_brand.py      # launcher icon and Fire TV banner
python3 tools/generate_artwork.py    # posters and backdrops, from the catalogue
python3 tools/generate_details.py    # cast, seasons and episodes
```

`app/src/main/assets/mock_catalog.json` is hand-authored and is the source of
truth — the artwork and detail scripts both read it, so they cannot drift from
the content. Fifty-six images come to 132 KB.

---

## What is built, and what is not

Built and green in CI:

- **Focus system** — a focused card rises, grows 5% and brightens over 200ms;
  the focused item settles into a fixed lane and the list moves beneath it;
  rows that do not hold focus recede; rows remember where the viewer left off.
- **Navigation** — a rail that expands on focus without shifting the content,
  and one Back rule: sections replace each other, details stack, Back from Home
  leaves.
- **Home** — cinematic hero that follows the focused card and stops working
  once it scrolls out of sight, over rows of content.
- **Movies and TV Shows** — paged grids that derive their column count from the
  width available.
- **Detail** — backdrop, poster, metadata, synopsis, cast, and for a series a
  season selector with episodes loaded a season at a time.
- **Library** — saving a title persists and survives a restart.

Not built. Deferred to phases 6–11, with their packages, provider interfaces
and persistence already in place:

- **Playback.** This is the visible gap. `PLAY`, `CONTINUE`, `START OVER`,
  `TRAILER` and selecting an episode all route through
  `NavigationHost.requestPlayback`, which currently logs and returns. The
  Media3 player attaches there.
- **Search**, **Library** and **Settings** screens. Reachable from the rail,
  where each says plainly that it is not built yet.
- The performance pass and final polish, once there is hardware to measure on.

---

## Design

Roughly 90% neutral to 10% accent. Surfaces are charcoal rather than pure
black, because true black crushes the elevation steps that carry depth and
bands visibly on the panels these devices are usually attached to. Text is a
warm off-white; pure white is harsh at ten feet in a dark room. The accent is a
muted amber, used only for selected states, progress and small highlights.

Type is system families only, on a scale with an 18sp floor for anything a
viewer is expected to read. Spacing is an 8dp grid inside a 5% overscan safe
area.

Nothing animates unless something changed. Durations are 150–280ms on a
decelerating curve, with no springs and no looping motion — an interface that
is still most of the time is what makes the movement mean something.
