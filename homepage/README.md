# Mango

A premium, Netflix-inspired streaming interface for Mango TV — cinematic,
dark, and built TV-first for Fire TV, with D-pad navigation as a first-class
concern rather than an afterthought.

It renders entirely on bundled mock content — no network, no accounts, no
backend — and is architected so that swapping the mock content for
Mango-TV's real data (or a Stremio-style add-on) later means implementing one
interface, not rewriting the UI.

**This copy is embedded in Mango-TV.** It is loaded by `WebViewHomeFragment`
from `file:///android_asset/homepage/`, which is why `vite.config.ts` builds
with a relative base and `main.tsx` uses `HashRouter` — see the parent
repository's own README under "Home is a WebView now" for the full story and
how to rebuild the bundle after changing anything here. An independent copy
of this project (without those two WebView-specific changes, and free to use
`BrowserRouter`/an absolute base again) lives at
github.com/MikeC444/Mango-Homepage.

---

## Running it

```
npm install
npm run dev      # http://localhost:5173
npm run build    # type-checks, then builds to dist/
npm run preview  # serve the production build locally
```

Navigate with a keyboard: **arrow keys** move focus, **Enter** activates,
**Escape** or **Backspace** goes back — the same three inputs a Fire TV
remote sends.

## Stack

React + TypeScript, built with Vite. Styling is plain CSS Modules over a
small set of design tokens (`src/styles/tokens.css`) — no CSS framework, no
component library, no animation library. Routing is `react-router-dom`.
State is local component state plus two small custom stores (My List and
playback progress, both backed by `localStorage`); there is no Redux/Zustand
— the app doesn't need one.

That minimalism is deliberate: the target hardware is a Fire TV Stick, which
can be significantly less powerful than the machine this was built on.
Fewer dependencies means less JS to parse and run before the first paint.

## How it is put together

```
src/
├── types/          Domain model (content.ts) and the ContentProvider
│                   interface (provider.ts) — the seam described below.
├── data/
│   ├── mock/       Bundled sample catalogue: titles, cast, genres, and a
│   │               procedural artwork palette (no binary image assets).
│   ├── local/      My List and playback progress, on localStorage.
│   ├── mockProvider.ts      Implements ContentProvider over the mock data.
│   ├── providerRegistry.ts  The one place the app decides which provider
│   │                        is active — swap the implementation here.
│   └── artwork.ts   Resolves an ArtworkRef + size to a URL (or, for the
│                    mock provider, to nothing — see components/common/Artwork).
├── navigation/      The D-pad spatial-navigation engine: a registry of
│                    focusable elements, nearest-neighbor geometry for
│                    arrow-key movement, and a Back-handler stack so modals
│                    and detail overlays can intercept Back before it leaves
│                    the page.
├── components/
│   ├── common/      Icon, Artwork, FocusContainer, Modal, LoadingState,
│                    ErrorState — the primitives everything else builds on.
│   ├── layout/      AppShell, TopNavigation, PageHeader.
│   ├── hero/        HeroBanner.
│   ├── content/     ContentRow, ContentCard (+ MovieCard/SeriesCard/
│                    ContinueWatchingCard wrappers), ContentGrid, ProgressBar.
│   ├── details/     CastList, SeasonSelector, EpisodeList.
│   └── search/      SearchBar (with an on-screen keyboard for remote input).
├── pages/           One component per route.
└── hooks/           useOutcome (Loading/Content/Empty/Error data fetching),
                     useMyList (reactive My List state).
```

### The seam: `ContentProvider`

Every screen reads content through `contentProvider` (from
`data/providerRegistry.ts`) and nothing else — no component imports mock
data or `localStorage` directly. `ContentProvider` (`src/types/provider.ts`)
defines catalog, detail, search, genre, My List, continue-watching, and
playback-info methods, each returning an `Outcome<T>` —
`loading | content | empty | error` — so a screen renders a clean state
without try/catch sprinkled through it.

Pointing Mango at real data later means writing a new class that implements
`ContentProvider` (against Mango-TV's catalog, or a Stremio-compatible
add-on) and changing one line in `providerRegistry.ts`. Nothing in
`components/` or `pages/` changes.

Artwork works the same way: content carries an opaque `ArtworkRef` key, and
`data/artwork.ts` resolves it to a URL for a given target size. The mock
provider has no real images, so it always resolves to nothing and
`components/common/Artwork` falls back to a deterministic, cinematic
gradient derived from the title's genre — no binary assets, no network
fetches, nothing to decode on a slow first paint.

### The D-pad navigation engine

`navigation/SpatialNavContext.tsx` keeps a registry of every currently
mounted focusable element (`useFocusable` in `navigation/useFocusable.ts`
registers on mount, unregisters on unmount) and one piece of state: which id
currently holds focus. On an arrow key it doesn't ask components which
neighbor is "next" — it measures every registered element's position with
`getBoundingClientRect()` and picks the nearest candidate in that direction,
weighted so movement stays predictable (moving down a column doesn't jump
sideways). This means a row, a grid, and the top nav all interoperate
automatically: nothing has to know it's sitting next to a differently-shaped
layout.

`FocusContainer` is the primitive every interactive control renders through.
It exposes the current focus state as `data-focused` on the DOM node, and
every component's CSS keys its scale/glow/elevation off that attribute —
never off `:hover`, since a remote never hovers.

Back is a stack, not a single handler: `AppShell` pushes a default "go to
the previous route" handler, and anything that should intercept Back first —
`Modal`, most notably — pushes its own (closing itself) and pops it on
unmount. This is also how Mango-TV's own rule reads: sections replace each
other, details and modals stack, Back unwinds them in order.

## What is built

- **Focus system** — nearest-neighbor D-pad navigation across rows, grids,
  and the top nav, with an explicit `data-focused` scale/glow/elevation
  state and no dead ends.
- **Navigation** — persistent top nav, section routes, a Back-handler stack.
- **Home** — a rotating cinematic hero over Netflix-style rows (Continue
  Watching, Trending, Popular Movies/TV, Recently Added, Top Rated, and one
  row per genre with enough content).
- **Movies / TV Shows / Genres** — browsable grids and genre tiles.
- **Details** — backdrop, poster, metadata, synopsis, cast, director/
  creators, and for a series a season selector with a full episode list
  (thumbnails, synopses, per-episode progress).
- **Search** — a TV-oriented on-screen keyboard plus type/movie/series
  filters, with physical-keyboard support for desktop use.
- **My List** — add/remove from any card or the details page, backed by
  `localStorage`, reflected live everywhere a title appears.
- **Settings** — Playback, Appearance, Language, Subtitles, Audio, Account,
  Add-ons, About, styled as a real settings surface rather than a generic
  Android/system settings screen.

Not built: actual video playback (there's no real media to play — selecting
Play opens a modal explaining where a `PlaybackSource` would hand off),
add-on installation by manifest URL, and account/profile management beyond
a placeholder.

## Design

Near-black surfaces (`#0a0908`, not pure black — pure black crushes
elevation and bands on the panels these devices are usually attached to),
warm off-white text, and a single muted mango-orange accent used only for
focus, selection, and small emphasis. Radii are small and restrained;
nothing is a pill except the search filters and season selector, which read
as controls rather than decoration. Motion is 150–340ms on a decelerating
curve — nothing animates unless something changed.
