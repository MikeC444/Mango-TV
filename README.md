# Mango TV

A streaming interface for Amazon Fire TV, built to feel like a piece of
consumer electronics rather than an app: cinematic, restrained, and fast on the
cheapest stick someone might own.

Kotlin, Views and RecyclerView, no Compose. Runs entirely on bundled mock
content — no network, no accounts, no backend.

The one exception is Home: it is a WebView loading `homepage/`, a separate
React/TypeScript frontend built with its own D-pad-navigation engine. See
[Home is a WebView now](#home-is-a-webview-now) below.

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
├── addon/        The Stremio protocol layer: manifests, catalogues, metadata,
│                 streams, subtitles - no Android dependencies, no UI.
├── pairing/      A local web server and QR code so a manifest URL can be
│                 typed on a phone instead of a television remote.
├── data/
│   ├── provider/ The seam: CatalogProvider, MovieProvider, SeriesProvider,
│   │             SearchProvider, StreamProvider, SubtitleProvider. Return
│   │             outcomes, never throw. Bundled content and add-ons both
│   │             implement it; nothing above this layer knows which answered.
│   ├── mock/     Bundled content behind those interfaces.
│   └── local/    Watchlist and playback positions, on DataStore.
├── repository/   Provider results mapped to Loading / Content / Empty / Error.
├── cache/        Image pipeline. Every sizing and decode rule lives here.
├── player/       PendingPlayback and PlayerActivity - Media3, subtitle
│                 tracks, resume position.
├── ui/
│   ├── core/     The focus engine and the design system.
│   ├── home/     WebViewHomeFragment - hosts homepage/'s built output.
│   ├── browse/   Movies and TV Shows grids.
│   ├── detail/   Film and series detail, cast, seasons, episodes.
│   ├── addon/    Installing, listing and managing add-ons.
│   ├── settings/ The Settings section itself.
│   ├── player/   The source picker shown when Play is pressed.
│   └── common/   Error, empty and placeholder states.
├── navigation/   One activity, fragments, a rail, and Back.
└── di/           A hand-written object graph. No annotation processor.
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

## Home is a WebView now

Home used to be a native Compose screen (`ui/home/`, a hero over content
rows). It has been replaced by `WebViewHomeFragment`, which loads
`homepage/` — a standalone React + TypeScript frontend with its own
cinematic design system and its own D-pad spatial-navigation engine, driven
by the arrow-key/Enter/Escape events a WebView already forwards from a Fire
TV remote. See `homepage/README.md` for how that side is built.

The Settings → Home Screen appearance editor (layout, rows, hero, presets,
live preview) was removed along with the native screen it customised — there
is no longer a native Home layout for it to configure. The parts of that
system other screens actually depended on (card corner radius and focus
effect, glass surfaces, colours, typography scale, accessibility) survive as
fixed defaults in `theme/ThemeDefaults.kt`, so Browse, Detail and Settings
keep exactly the look they had; there is just no longer a settings screen to
change it from.

**Rebuilding the bundle** — the built output is committed to
`app/src/main/assets/homepage/`, the same convention `tools/generate_*.py`
already uses for the bundled artwork, so a normal Gradle build needs no
Node.js step:

```
cd homepage
npm install
npm run build
rm -rf ../app/src/main/assets/homepage
cp -r dist ../app/src/main/assets/homepage
```

Two things are specific to running inside this WebView rather than a normal
browser, both already applied in `homepage/`: `vite.config.ts` builds with
`base: './'` (an absolute base would resolve against the filesystem root
under `file://`, not this directory), and `main.tsx` uses `HashRouter` rather
than `BrowserRouter` (there is no server to fall back an arbitrary path to
`index.html` the way the History API needs).

---

## What is built, and what is not

Built and green in CI:

- **Focus system** (native screens) — a focused card rises, grows 5% and
  brightens over 200ms; rows that do not hold focus recede; rows remember
  where the viewer left off. Home has its own separate, web-based focus
  engine — see [Home is a WebView now](#home-is-a-webview-now).
- **Navigation** — a rail that expands on focus without shifting the content,
  and one Back rule: sections replace each other, details stack, Back from Home
  leaves.
- **Home** — a WebView-hosted cinematic hero and content rows, with its own
  D-pad navigation, browsing, search, My List and settings surface.
- **Movies and TV Shows** — paged grids that derive their column count from the
  width available.
- **Detail** — backdrop, poster, metadata, synopsis, cast, and for a series a
  season selector with episodes loaded a season at a time.
- **Library** — saving a title persists and survives a restart.
- **Stremio-compatible add-ons** — install by manifest URL (typed, or via a
  QR code and a phone), browse and open their content through the same
  screens the bundled catalogue uses, configure one that requires it, reorder
  and remove from an add-on's own detail screen.
- **Playback.** `PLAY`, `CONTINUE`, `START OVER` and selecting an episode all
  route through `NavigationHost.requestPlayback`, which opens a source picker
  querying every enabled add-on for a stream and ranking what comes back; the
  viewer chooses, and a Media3 player plays it, with subtitle tracks attached
  and position saved as it plays.

Not built:

- **Search** and **Library** screens. Reachable from the rail, where each says
  plainly that it is not built yet.
- **Trailer.** Routes through the same picker as everything else, but nothing
  distinguishes a trailer stream from a full one yet.
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
