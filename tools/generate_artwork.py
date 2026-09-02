#!/usr/bin/env python3
"""
Generates Mango TV's bundled placeholder artwork.

The application ships with artwork so that focus, scrolling and image-cache
behaviour can be exercised on real hardware without a network, and so the
interface can be judged as it will actually look rather than as grey
rectangles. These are abstract cinematic fields rather than fake film stills:
they read as photography at a distance, carry the muted palette the interface
is built around, and cost a fraction of the bytes a photograph would.

Each title's artwork is derived deterministically from its id, so a title looks
the same on every run and regenerating never produces a spurious diff.

Everything is drawn as smooth gradients with a little grain, which is close to
the best case for WebP - the whole set compresses to well under a megabyte.

Run from the repository root:  python3 tools/generate_artwork.py
Requires Pillow and numpy. Output is committed; rerun only when the look
changes or titles are added.
"""

import hashlib
import json
import os

import numpy as np
from PIL import Image, ImageFilter

# Poster artwork is generated at exactly the pixel size the card occupies at
# xhdpi, so a poster is never decoded larger than it is drawn.
POSTER_W, POSTER_H = 296, 444

# Backdrops are capped here regardless of how large the hero is drawn. An
# abstract field carries no fine detail to lose, and a 4K backdrop held in
# memory is the single fastest way to exhaust a Fire Stick's heap.
BACKDROP_W, BACKDROP_H = 1280, 720

QUALITY = 72

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
OUT_DIR = os.path.join(ASSETS, "artwork")
CATALOG_PATH = os.path.join(ASSETS, "mock_catalog.json")

# Muted cinematic families. Every one of these sits comfortably beside a
# charcoal interface and none of them competes with the amber accent.
PALETTES = [
    # deep base, mid tone, light haze
    ((14, 18, 26), (46, 62, 82), (146, 165, 184)),    # blue dusk
    ((22, 17, 13), (74, 55, 38), (196, 166, 126)),    # umber
    ((12, 20, 19), (38, 66, 60), (140, 172, 160)),    # forest
    ((17, 17, 19), (58, 58, 63), (168, 166, 162)),    # monochrome
    ((19, 15, 20), (58, 46, 58), (150, 134, 146)),    # plum
    ((13, 19, 24), (52, 74, 84), (168, 190, 196)),    # slate teal
    ((24, 19, 16), (84, 62, 46), (208, 178, 142)),    # sand
]


def seed_of(key):
    """A stable integer for a title id, independent of Python's hash seed."""
    return int(hashlib.sha256(key.encode()).hexdigest()[:8], 16)


def vertical_ramp(w, h, top, bottom, horizon, softness):
    """
    A two-tone field split by a soft horizon.

    The horizon is what makes these read as landscape rather than as a plain
    gradient - the eye takes a tonal break across the frame as depth.
    """
    y = np.linspace(0.0, 1.0, h, dtype=np.float32)[:, None]
    # A logistic edge rather than a hard line, so the break has atmosphere.
    t = 1.0 / (1.0 + np.exp(-(y - horizon) / max(softness, 1e-3)))
    top = np.array(top, dtype=np.float32)
    bottom = np.array(bottom, dtype=np.float32)
    field = top[None, None, :] * (1.0 - t[..., None]) + bottom[None, None, :] * t[..., None]
    return np.repeat(field, w, axis=1)


def add_glow(field, cx, cy, radius, colour, strength):
    """A soft light source: the sun low in the frame, or a light off-camera."""
    h, w, _ = field.shape
    ys = np.linspace(0.0, 1.0, h, dtype=np.float32)[:, None]
    xs = np.linspace(0.0, 1.0, w, dtype=np.float32)[None, :]
    # Aspect-corrected so the glow stays circular in a tall poster.
    aspect = w / float(h)
    d = np.sqrt(((xs - cx) * aspect) ** 2 + (ys - cy) ** 2)
    falloff = np.clip(1.0 - d / radius, 0.0, 1.0) ** 2
    colour = np.array(colour, dtype=np.float32)
    return field + falloff[..., None] * colour[None, None, :] * strength


def add_band(field, centre, thickness, colour, strength):
    """A horizontal haze band - cloud, mist, or a shaft of light."""
    h, w, _ = field.shape
    y = np.linspace(0.0, 1.0, h, dtype=np.float32)[:, None]
    band = np.exp(-((y - centre) ** 2) / (2.0 * thickness ** 2))
    colour = np.array(colour, dtype=np.float32)
    return field + np.repeat(band, w, axis=1)[..., None] * colour[None, None, :] * strength


def vignette(field, strength):
    """Draws the eye inward and keeps the frame from ending on a bright edge."""
    h, w, _ = field.shape
    ys = np.linspace(-1.0, 1.0, h, dtype=np.float32)[:, None]
    xs = np.linspace(-1.0, 1.0, w, dtype=np.float32)[None, :]
    d = np.sqrt(xs ** 2 + ys ** 2) / np.sqrt(2.0)
    return field * (1.0 - strength * d[..., None] ** 2)


def grain(field, rng, amount):
    """
    Fine noise.

    Cosmetic, but also practical: a smooth gradient across a large area bands
    visibly on the eight-bit panels these devices are usually attached to, and
    a little noise dithers the steps away.
    """
    return field + rng.normal(0.0, amount, field.shape).astype(np.float32)


def soft_masses(field, rng, base_colour, count, low_bias):
    """
    Large, heavily out-of-focus dark forms.

    An earlier version drew hard silhouettes - ridgelines and skylines. They
    gave the frame structure but they also named a subject, and a row of
    recognisable mountains and city blocks reads as clip art rather than as
    artwork.

    Soft masses do the same job without the literalism: they establish a
    foreground plane and a depth relationship, and stay abstract enough that
    the eye accepts them as something photographed slightly out of focus.
    """
    h, w, _ = field.shape
    ys = np.linspace(0.0, 1.0, h, dtype=np.float32)[:, None]
    xs = np.linspace(0.0, 1.0, w, dtype=np.float32)[None, :]

    mask = np.zeros((h, w), dtype=np.float32)
    for _ in range(count):
        cx = float(rng.uniform(-0.15, 1.15))
        # Foreground mass sits low in the frame, the way a real one would.
        cy = float(rng.uniform(low_bias, 1.25))
        sx = float(rng.uniform(0.18, 0.55))
        sy = float(rng.uniform(0.16, 0.48))
        blob = np.exp(-(((xs - cx) / sx) ** 2 + ((ys - cy) / sy) ** 2))
        mask = np.maximum(mask, blob)

    mask = np.clip(mask * float(rng.uniform(0.85, 1.15)), 0.0, 1.0)
    shadow = np.array(base_colour, dtype=np.float32) * float(rng.uniform(0.35, 0.62))
    return field * (1.0 - mask[..., None]) + shadow[None, None, :] * mask[..., None]


def side_light(field, rng):
    """A gentle left-to-right or right-to-left falloff, for a light direction."""
    h, w, _ = field.shape
    xs = np.linspace(0.0, 1.0, w, dtype=np.float32)[None, :]
    if rng.random() < 0.5:
        xs = 1.0 - xs
    ramp = 1.0 - float(rng.uniform(0.12, 0.30)) * xs
    return field * np.repeat(ramp, h, axis=0)[..., None]


def desaturate(field, amount):
    """
    Pulls every frame part-way toward its own luminance.

    The interface is built on a near-neutral palette with a single restrained
    accent, and artwork is the largest coloured surface on the screen. Left at
    full saturation it competes with the accent and reads as a different
    product; muted, it reads as photography sitting inside the interface.
    """
    luminance = field.mean(axis=2, keepdims=True)
    return field * (1.0 - amount) + luminance * amount


def normalise_exposure(field, rng):
    """
    Brings every frame into a legible band.

    Composing from random light strengths and multipliers produces some frames
    so dark they read as artwork that failed to load, and others bright enough
    to glare against a charcoal interface. Rather than hand-tuning the
    constants until neither happens, the finished field is graded: scaled so
    its average sits at a target, then checked to make sure it still has a
    highlight somewhere.

    The target itself varies a little per title, because a row where every card
    is exposed identically is its own kind of monotony.
    """
    target_mean = float(rng.uniform(38.0, 56.0))
    target_high = float(rng.uniform(115.0, 155.0))

    luminance = field.mean(axis=2)
    field = field * (target_mean / max(float(luminance.mean()), 1e-3))

    # Without a highlight the frame reads as a flat wash rather than as
    # something lit.
    highlight = float(np.percentile(field.mean(axis=2), 98.0))
    if highlight < target_high:
        field = field * (target_high / max(highlight, 1e-3))

    return field


def compose(w, h, key, kind):
    rng = np.random.default_rng(seed_of(key + kind))
    base, mid, light = PALETTES[seed_of(key) % len(PALETTES)]

    wide = w > h

    # Four lighting archetypes, so a row of cards has variety of structure
    # rather than seven exposures of the same scene.
    archetype = int(rng.integers(0, 4))
    if archetype == 0:      # high sky, light low in the frame
        horizon = float(rng.uniform(0.60, 0.76))
        glow_strength = float(rng.uniform(0.26, 0.42))
    elif archetype == 1:    # low, heavy sky pressing down
        horizon = float(rng.uniform(0.26, 0.40))
        glow_strength = float(rng.uniform(0.12, 0.26))
    elif archetype == 2:    # backlit, the source behind the subject
        horizon = float(rng.uniform(0.48, 0.64))
        glow_strength = float(rng.uniform(0.40, 0.62))
    else:                   # overcast, almost no source at all
        horizon = float(rng.uniform(0.42, 0.58))
        glow_strength = float(rng.uniform(0.06, 0.16))

    field = vertical_ramp(w, h, light, base, horizon, softness=float(rng.uniform(0.07, 0.18)))

    field = add_glow(
        field,
        cx=float(rng.uniform(0.15, 0.85)),
        cy=horizon - float(rng.uniform(0.0, 0.12)),
        radius=float(rng.uniform(0.30, 0.62)),
        colour=light,
        strength=glow_strength,
    )

    for _ in range(int(rng.integers(1, 3))):
        field = add_band(
            field,
            centre=float(rng.uniform(0.10, max(horizon - 0.05, 0.12))),
            thickness=float(rng.uniform(0.04, 0.10)),
            colour=mid,
            strength=float(rng.uniform(0.12, 0.26)),
        )

    field = side_light(field, rng)

    # A wide frame carries its weight at the edges; a tall one carries it along
    # the bottom.
    field = soft_masses(
        field,
        rng,
        base,
        count=int(rng.integers(2, 4)),
        low_bias=0.72 if wide else 0.62,
    )

    field = vignette(field, strength=float(rng.uniform(0.34, 0.50)))
    field = desaturate(field, amount=0.22)
    field = normalise_exposure(field, rng)
    field = grain(field, rng, amount=2.2)

    img = Image.fromarray(np.clip(field, 0, 255).astype(np.uint8), "RGB")
    return img.filter(ImageFilter.GaussianBlur(0.5))


def main():
    os.makedirs(OUT_DIR, exist_ok=True)

    # Ids come from the catalogue itself rather than being repeated here, so
    # artwork and content cannot drift apart: adding a title to the JSON and
    # rerunning is all it takes.
    with open(CATALOG_PATH, encoding="utf-8") as handle:
        catalog = json.load(handle)
    ids = [title["id"] for title in catalog["titles"]]

    # Every title needs a backdrop, not just the featured ones: the hero
    # follows the focused card, so any title can end up filling the screen.
    total = 0
    for media_id in ids:
        for kind, size in (
            ("poster", (POSTER_W, POSTER_H)),
            ("backdrop", (BACKDROP_W, BACKDROP_H)),
        ):
            path = os.path.join(OUT_DIR, f"{kind}_{media_id}.webp")
            compose(size[0], size[1], media_id, kind).save(
                path, "WEBP", quality=QUALITY, method=6
            )
            total += os.path.getsize(path)

    print(f"{len(ids)} titles, {len(ids) * 2} images")
    print(f"total {total / 1024:.0f} KB in {os.path.relpath(OUT_DIR, ROOT)}")


if __name__ == "__main__":
    main()
