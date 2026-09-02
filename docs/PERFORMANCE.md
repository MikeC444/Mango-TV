# Performance

Mango TV targets entry-level Fire TV Stick hardware, where the binding
constraint is memory rather than clock speed. This document records the
decisions taken for performance, and the checklist to measure them against on
a real device.

**Nothing here has been measured yet.** The application has been built and
linted in CI but has not run on Fire TV hardware, and no emulator was available
during development. Every number below is a target to test against, not a
result.

## Decisions taken, and why

### Views rather than Compose

The whole interface is built on Views and RecyclerView. Skipping the Compose
runtime removes its class loading and first-composition cost from cold start
and keeps the APK and the heap smaller, and view recycling is well proven on
this class of device.

*This is a judgement, not a measurement.* If a Compose build with a Baseline
Profile turns out to start and scroll as well on the actual target device, the
reasoning here should be revisited.

### One shadow at a time

Only the focused card is ever elevated. Real-time shadows are among the more
expensive things a GPU of this generation does, and at most one card per row
holds focus, so at most one shadow is being cast.

### Focus animation costs one animator

The scale, the lift and the brightness change are one `ViewPropertyAnimator`,
with the overlay's alpha driven from the scale the animator is already
producing. Any in-flight animation is cancelled before a new one starts, so
holding a direction on the remote cannot stack animators on a view.

### Scaled surfaces carry no text

Poster cards have no title beneath them. Scaling a view that draws text
re-rasterises the glyphs every frame; scaling one that draws a bitmap is a
transform the GPU applies for free. Episode cards do carry text, and opt into
being rasterised into a layer for the duration of the animation instead.

### Dimming is per row, not per card

Rows that do not hold focus recede to 65%. Applied at row level, a focus change
costs two animations regardless of how many cards are on screen.
`RowContainerView` overrides `hasOverlappingRendering()` to return false, which
is what stops that alpha allocating an off-screen layer per row - a row's
header and cards never overlap, so per-child alpha is identical and free.

### Cards are shared across the whole screen

Every row draws from one `RecycledViewPool` owned by the vertical list, so a
card scrolled off one row is immediately reusable by another. The per-list view
cache is left at its default, because it holds recently bound cards for the
immediate return that scrolling back along a row performs.

### Images are decoded at the size they are drawn

Every request states its target in pixels. Posters decode as `RGB_565`, halving
the bytes per pixel; the artwork has no transparency and no gradient fine
enough for the loss to show at poster size. Backdrops keep full colour depth
but are capped at 1280x720 regardless of the television's resolution.

### The hero stops working when it cannot be seen

Once the rows have scrolled over it, focus changes stop touching the hero
entirely - no text updates and no backdrop decode. Updates are debounced by
220ms, because holding a direction crosses roughly a dozen cards a second.

### The navigation rail never re-lays out

The rail is always its full width and draws over the content when opened, so
expanding it is two alpha fades rather than a measure and layout pass per
frame. Content never shifts.

### Nothing heavy at startup

`Application.onCreate` builds a lazy object graph and nothing else. No database
is opened, no image loader is initialised, no asset is read. The bundled
catalogue is parsed on a background dispatcher on first use, and the larger
detail payload is a separate asset that many sessions never touch.

### Overdraw is avoided by omission

The base surface is painted once, by the window background. Fragment roots and
the content container deliberately have no background of their own.

## Checklist to run on a device

Measure on the lowest-powered target available, not on a development machine.

| What | How | Target |
|---|---|---|
| Cold start to first frame | `adb shell am start -W -n tv.mango.app/.navigation.MainActivity`, read `TotalTime` | under 2s |
| Home content on screen | Stopwatch from launch to rows drawn | under 2.5s |
| Focus movement latency | Hold right on a row; watch for lag between press and lift | no perceptible delay |
| Row scrolling | `adb shell dumpsys gfxinfo tv.mango.app framestats` while scrolling | 90th percentile under 16ms |
| Screen transition | Home to detail and back | under 400ms to content |
| Memory, home screen | `adb shell dumpsys meminfo tv.mango.app`, read `TOTAL PSS` | under 120 MB |
| Memory after browsing | Same, after ten minutes across every screen | no sustained growth |
| Leaks | Repeatedly open and close detail screens, watch PSS | returns to baseline |
| Image cache | `dumpsys meminfo`, read the Graphics and Native lines | stable while scrolling |

## If something is slow

In order of what to try first:

1. **Confirm which frames are slow** with `gfxinfo framestats` before changing
   anything. The expensive thing is rarely the thing that looks expensive.
2. **Drop the focus scale** from 1.05 to 1.03 before removing it. The lift is
   what makes the interface feel physical; the size of it is negotiable.
3. **Reduce the recycled view pool** if memory rather than frame time is the
   problem. It is set to 24 cards.
4. **Take the row dimming out.** It is the only per-frame alpha work on the
   screen and it is the least load-bearing effect in the interface.
5. **Replace the elevated shadow** on the focused card with a pre-rendered
   nine-patch. This trades GPU work for a small amount of memory.

Performance wins over any of these effects. If one of them costs frames on the
target device, simplify it or take it out.
